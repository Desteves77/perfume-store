package com.perfumestore.backend.inventory;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.perfumestore.backend.catalog.PresentationRepository;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.inventory.InventoryAvailabilityReader;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService implements InventoryAvailabilityReader {

    private static final Pattern OPERATION_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

    private final PresentationRepository presentations;
    private final InventoryBalanceRepository balances;
    private final InventoryMovementRepository movements;
    private final StockReservationRepository reservations;

    public InventoryService(PresentationRepository presentations, InventoryBalanceRepository balances,
            InventoryMovementRepository movements, StockReservationRepository reservations) {
        this.presentations = presentations;
        this.balances = balances;
        this.movements = movements;
        this.reservations = reservations;
    }

    @Transactional
    public InventoryDtos.BalanceResponse adjust(UUID presentationId, int quantity, InventoryMovementType type,
            String reason, String operationKey, UUID actorUserId, String correlationId) {
        requirePresentation(presentationId);
        validateOperationKey(operationKey);
        if (type != InventoryMovementType.ENTRY && type != InventoryMovementType.ADJUSTMENT
                && type != InventoryMovementType.RESTOCK) {
            throw conflict("INVENTORY_MOVEMENT_TYPE_INVALID", "O tipo não representa uma entrada ou ajuste administrativo.");
        }
        if (quantity == 0 || ((type == InventoryMovementType.ENTRY || type == InventoryMovementType.RESTOCK) && quantity < 0)) {
            throw conflict("INVENTORY_QUANTITY_INVALID", (type == InventoryMovementType.ENTRY || type == InventoryMovementType.RESTOCK)
                    ? "A quantidade da entrada deve ser positiva."
                    : "O ajuste não pode ter quantidade zero.");
        }
        String normalizedReason = cleanReason(reason);
        balances.ensure(presentationId, Instant.now());
        InventoryBalanceRecord current = lockBalance(presentationId);
        InventoryMovementRecord previous = movements.findByOperationKey(operationKey).orElse(null);
        if (previous != null) {
            ensureSameAdjustment(previous, presentationId, quantity, type, normalizedReason, actorUserId);
            return balanceResponse(current);
        }
        int physicalAfter;
        try {
            physicalAfter = Math.addExact(current.physicalQuantity(), quantity);
        } catch (ArithmeticException exception) {
            throw conflict("INVENTORY_LIMIT_EXCEEDED", "A quantidade ultrapassa o limite suportado pelo estoque.");
        }
        if (physicalAfter < current.reservedQuantity()) {
            throw conflict("INVENTORY_BELOW_RESERVED",
                    "O estoque físico não pode ficar abaixo da quantidade reservada.");
        }
        Instant now = Instant.now();
        balances.update(presentationId, physicalAfter, current.reservedQuantity(), current.version(), now);
        InventoryMovementRecord movement = new InventoryMovementRecord(UUID.randomUUID(), presentationId, type,
                quantity, 0, current.physicalQuantity(), physicalAfter, current.reservedQuantity(),
                current.reservedQuantity(), normalizedReason, operationKey, null, actorUserId,
                cleanCorrelation(correlationId), now);
        if (!movements.insertIfAbsent(movement)) {
            throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de operação já foi utilizada.");
        }
        return balanceResponse(balances.find(presentationId).orElseThrow());
    }

    @Transactional
    public InventoryDtos.ReservationResponse reserve(UUID presentationId, int quantity, String reservationKey,
            UUID orderId, Instant expiresAt, UUID actorUserId, String correlationId) {
        requirePresentation(presentationId);
        validateReservationKey(reservationKey);
        if (quantity <= 0) {
            throw conflict("RESERVATION_QUANTITY_INVALID", "A quantidade reservada deve ser positiva.");
        }
        if (!presentations.existsActive(presentationId)) {
            throw conflict("PRESENTATION_INACTIVE", "A apresentação está inativa e não pode ser reservada.");
        }
        StockReservationRecord existing = reservations.findForUpdate(reservationKey).orElse(null);
        if (existing != null) {
            ensureSameReservation(existing, presentationId, quantity, orderId, expiresAt);
            return reservationResponse(existing);
        }
        balances.ensure(presentationId, Instant.now());
        InventoryBalanceRecord current = lockBalance(presentationId);
        if (current.availableQuantity() < quantity) {
            throw conflict("INSUFFICIENT_STOCK", "A apresentação não possui quantidade disponível suficiente.");
        }
        UUID reservationId = UUID.randomUUID();
        Instant now = Instant.now();
        if (!reservations.insertIfAbsent(reservationId, reservationKey, presentationId, quantity, orderId,
                expiresAt, now)) {
            StockReservationRecord concurrent = reservations.findForUpdate(reservationKey).orElseThrow();
            ensureSameReservation(concurrent, presentationId, quantity, orderId, expiresAt);
            return reservationResponse(concurrent);
        }
        int reservedAfter;
        try {
            reservedAfter = Math.addExact(current.reservedQuantity(), quantity);
        } catch (ArithmeticException exception) {
            throw conflict("INVENTORY_LIMIT_EXCEEDED", "A quantidade ultrapassa o limite suportado pelo estoque.");
        }
        Instant changedAt = Instant.now();
        balances.update(presentationId, current.physicalQuantity(), reservedAfter, current.version(), changedAt);
        String movementKey = reservationKey + ":reserve";
        InventoryMovementRecord movement = new InventoryMovementRecord(UUID.randomUUID(), presentationId,
                InventoryMovementType.RESERVATION, 0, quantity, current.physicalQuantity(), current.physicalQuantity(),
                current.reservedQuantity(), reservedAfter, "Reserva de estoque", movementKey, reservationId,
                actorUserId, cleanCorrelation(correlationId), changedAt);
        movements.insertIfAbsent(movement);
        return reservationResponse(reservations.findByKey(reservationKey).orElseThrow());
    }

    @Transactional
    public InventoryDtos.BalanceResponse confirm(String reservationKey, UUID actorUserId, String correlationId) {
        StockReservationRecord reservation = lockReservation(reservationKey);
        if (reservation.state() == ReservationState.CONFIRMED) {
            return balance(presentationId(reservation));
        }
        if (reservation.state() == ReservationState.RELEASED) {
            throw conflict("RESERVATION_ALREADY_RELEASED", "A reserva já foi liberada e não pode ser confirmada.");
        }
        InventoryBalanceRecord current = lockBalance(reservation.presentationId());
        if (current.reservedQuantity() < reservation.quantity() || current.physicalQuantity() < reservation.quantity()) {
            throw conflict("RESERVATION_BALANCE_INCONSISTENT", "A reserva não pode ser confirmada com o saldo atual.");
        }
        int physicalAfter = current.physicalQuantity() - reservation.quantity();
        int reservedAfter = current.reservedQuantity() - reservation.quantity();
        Instant now = Instant.now();
        balances.update(reservation.presentationId(), physicalAfter, reservedAfter, current.version(), now);
        reservations.updateState(reservation.id(), ReservationState.CONFIRMED, reservation.version(), now);
        InventoryMovementRecord movement = new InventoryMovementRecord(UUID.randomUUID(), reservation.presentationId(),
                InventoryMovementType.CONFIRMATION, -reservation.quantity(), -reservation.quantity(),
                current.physicalQuantity(), physicalAfter, current.reservedQuantity(), reservedAfter,
                "Confirmação de reserva", reservationKey + ":confirm", reservation.id(), actorUserId,
                cleanCorrelation(correlationId), now);
        movements.insertIfAbsent(movement);
        return balanceResponse(balances.find(reservation.presentationId()).orElseThrow());
    }

    @Transactional
    public InventoryDtos.BalanceResponse release(String reservationKey, UUID actorUserId, String correlationId) {
        StockReservationRecord reservation = lockReservation(reservationKey);
        if (reservation.state() == ReservationState.RELEASED) {
            return balance(presentationId(reservation));
        }
        if (reservation.state() == ReservationState.CONFIRMED) {
            throw conflict("RESERVATION_ALREADY_CONFIRMED", "A reserva já foi confirmada e não pode ser liberada.");
        }
        InventoryBalanceRecord current = lockBalance(reservation.presentationId());
        if (current.reservedQuantity() < reservation.quantity()) {
            throw conflict("RESERVATION_BALANCE_INCONSISTENT", "A reserva não pode ser liberada com o saldo atual.");
        }
        int reservedAfter = current.reservedQuantity() - reservation.quantity();
        Instant now = Instant.now();
        balances.update(reservation.presentationId(), current.physicalQuantity(), reservedAfter, current.version(), now);
        reservations.updateState(reservation.id(), ReservationState.RELEASED, reservation.version(), now);
        InventoryMovementRecord movement = new InventoryMovementRecord(UUID.randomUUID(), reservation.presentationId(),
                InventoryMovementType.RELEASE, 0, -reservation.quantity(), current.physicalQuantity(),
                current.physicalQuantity(), current.reservedQuantity(), reservedAfter, "Liberação de reserva",
                reservationKey + ":release", reservation.id(), actorUserId, cleanCorrelation(correlationId), now);
        movements.insertIfAbsent(movement);
        return balanceResponse(balances.find(reservation.presentationId()).orElseThrow());
    }

    @Transactional
    public InventoryDtos.BalanceResponse balance(UUID presentationId) {
        requirePresentation(presentationId);
        balances.ensure(presentationId, Instant.now());
        return balanceResponse(lockBalance(presentationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> availableByPresentationIds(Collection<UUID> presentationIds) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        balances.findForPresentations(presentationIds).forEach(record -> result.put(record.presentationId(),
                record.availableQuantity()));
        presentationIds.forEach(id -> result.putIfAbsent(id, 0));
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryDtos.MovementResponse> movements(PageQuery page, UUID presentationId) {
        MovementSort sort = parseSort(page.getSort());
        PageResponse<InventoryMovementRecord> result = movements.findPage(page, presentationId,
                sort.sqlColumn(), sort.direction() == SortOrder.Direction.DESC ? "desc" : "asc");
        return new PageResponse<>(result.items().stream().map(this::movementResponse).toList(), result.page(),
                result.size(), result.totalElements(), result.totalPages(),
                List.of(new SortOrder(sort.apiName(), sort.direction()), new SortOrder("id", SortOrder.Direction.DESC)));
    }

    private InventoryBalanceRecord lockBalance(UUID presentationId) {
        return balances.findForUpdate(presentationId)
                .orElseThrow(() -> new ApiRequestException(500, "INVENTORY_BALANCE_MISSING",
                        "O saldo da apresentação não está disponível."));
    }

    private StockReservationRecord lockReservation(String reservationKey) {
        validateReservationKey(reservationKey);
        return reservations.findForUpdate(reservationKey).orElseThrow(() ->
                new ApiRequestException(404, "RESERVATION_NOT_FOUND", "A reserva não foi encontrada."));
    }

    private UUID presentationId(StockReservationRecord reservation) {
        return reservation.presentationId();
    }

    private void requirePresentation(UUID presentationId) {
        if (presentationId == null || presentations.findById(presentationId).isEmpty()) {
            throw new ApiRequestException(404, "PRESENTATION_NOT_FOUND", "A apresentação não foi encontrada.");
        }
    }

    private void ensureSameAdjustment(InventoryMovementRecord previous, UUID presentationId, int quantity,
            InventoryMovementType type, String reason, UUID actorUserId) {
        if (!previous.presentationId().equals(presentationId) || previous.physicalDelta() != quantity
                || previous.reservedDelta() != 0 || previous.type() != type || !previous.reason().equals(reason)
                || (previous.actorUserId() != null && !previous.actorUserId().equals(actorUserId))) {
            throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de operação foi usada com dados diferentes.");
        }
    }

    private void ensureSameReservation(StockReservationRecord existing, UUID presentationId, int quantity,
            UUID orderId, Instant expiresAt) {
        if (!existing.presentationId().equals(presentationId) || existing.quantity() != quantity
                || (existing.orderId() == null ? orderId != null : !existing.orderId().equals(orderId))
                || (existing.expiresAt() == null ? expiresAt != null : !existing.expiresAt().equals(expiresAt))) {
            throw conflict("IDEMPOTENCY_KEY_REUSED", "A chave de reserva foi usada com dados diferentes.");
        }
    }

    private void validateOperationKey(String key) {
        if (key == null || !OPERATION_KEY.matcher(key).matches()) {
            throw conflict("IDEMPOTENCY_KEY_INVALID", "Informe uma chave de operação válida.");
        }
    }

    private void validateReservationKey(String key) {
        if (key == null || key.length() > 70 || !Pattern.compile("[A-Za-z0-9._:-]{8,70}").matcher(key).matches()) {
            throw conflict("IDEMPOTENCY_KEY_INVALID", "Informe uma chave de reserva válida.");
        }
    }

    private String cleanReason(String reason) {
        if (reason == null || reason.trim().length() < 3 || reason.trim().length() > 255) {
            throw conflict("INVENTORY_REASON_REQUIRED", "Informe um motivo entre 3 e 255 caracteres.");
        }
        return reason.trim();
    }

    private String cleanCorrelation(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? null
                : correlationId.substring(0, Math.min(correlationId.length(), 64));
    }

    private MovementSort parseSort(String raw) {
        if (raw == null || raw.isBlank() || "id,asc".equals(raw)) {
            return new MovementSort("createdAt", "created_at", SortOrder.Direction.DESC);
        }
        String[] pieces = raw.split(",", -1);
        Map<String, String> allowed = Map.of("createdAt", "created_at", "type", "movement_type");
        if (pieces.length != 2 || !allowed.containsKey(pieces[0].trim())) {
            throw new ApiRequestException(400, "INVALID_SORT", "A ordenação solicitada não é permitida.");
        }
        try {
            String apiName = pieces[0].trim();
            return new MovementSort(apiName, allowed.get(apiName),
                    SortOrder.Direction.valueOf(pieces[1].trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new ApiRequestException(400, "INVALID_SORT", "A ordenação solicitada não é permitida.");
        }
    }

    private record MovementSort(String apiName, String sqlColumn, SortOrder.Direction direction) { }

    private InventoryDtos.BalanceResponse balanceResponse(InventoryBalanceRecord record) {
        return new InventoryDtos.BalanceResponse(record.presentationId(), record.physicalQuantity(),
                record.reservedQuantity(), record.availableQuantity(), record.version(), record.createdAt(),
                record.updatedAt());
    }

    private InventoryDtos.ReservationResponse reservationResponse(StockReservationRecord record) {
        return new InventoryDtos.ReservationResponse(record.id(), record.reservationKey(), record.presentationId(),
                record.quantity(), record.state(), record.orderId(), record.expiresAt(), record.createdAt(),
                record.updatedAt(), record.version());
    }

    private InventoryDtos.MovementResponse movementResponse(InventoryMovementRecord record) {
        return new InventoryDtos.MovementResponse(record.id(), record.presentationId(), record.type(),
                record.physicalDelta(), record.reservedDelta(), record.physicalBefore(), record.physicalAfter(),
                record.reservedBefore(), record.reservedAfter(), record.reason(), record.operationKey(),
                record.reservationId(), record.actorUserId(), record.correlationId(), record.createdAt());
    }

    private ApiRequestException conflict(String code, String message) {
        return new ApiRequestException(409, code, message);
    }
}
