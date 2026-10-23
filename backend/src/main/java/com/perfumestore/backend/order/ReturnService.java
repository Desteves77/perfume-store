package com.perfumestore.backend.order;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.inventory.InventoryMovementType;
import com.perfumestore.backend.inventory.InventoryService;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {
    private final ReturnRepository returns;
    private final OrderRepository orders;
    private final InventoryService inventory;

    public ReturnService(ReturnRepository returns, OrderRepository orders, InventoryService inventory) {
        this.returns = returns; this.orders = orders; this.inventory = inventory;
    }

    @Transactional
    public ReturnDtos.Detail request(String publicNumber, ReturnDtos.CreateRequest request, UUID actor,
            String key) {
        String normalizedKey = key == null || key.isBlank() ? "return:" + digest(publicNumber + "|" + request.orderItemId() + "|" + request.quantity() + "|" + request.reason()) : validateKey(key);
        OrderRecord order = lockOrder(publicNumber);
        ReturnRecord existing = returns.findByOrderAndKeyForUpdate(order.id(), normalizedKey).orElse(null);
        if (existing != null) return detail(existing);
        if (order.deliveryStatus() != DeliveryStatus.SHIPPED && order.deliveryStatus() != DeliveryStatus.DELIVERED) {
            throw conflict("RETURN_NOT_ELIGIBLE", "O pedido ainda nÃ£o estÃ¡ em estado elegÃ­vel para devoluÃ§Ã£o.");
        }
        OrderItemRecord item = orders.items(order.id()).stream().filter(i -> i.id().equals(request.orderItemId())).findFirst().orElseThrow(() ->
                new ApiRequestException(404, "ORDER_ITEM_NOT_FOUND", "O item do pedido nÃ£o foi encontrado."));
        int prior = jdbcSumForItem(item.id());
        if (prior + request.quantity() > item.quantity()) throw conflict("RETURN_QUANTITY_EXCEEDED", "A quantidade devolvida excede a quantidade comprada.");
        Instant now = Instant.now();
        ReturnRecord record = new ReturnRecord(UUID.randomUUID(), order.id(), item.id(), item.presentationId(), request.quantity(), 0, 0, 0,
                request.reason().trim(), ReturnStatus.REQUESTED, actor, null, null, now, null, null, null, null, normalizedKey, now);
        returns.insert(record);
        returns.history(record.id(), null, ReturnStatus.REQUESTED.name(), "RETURN_REQUESTED", actor, record.reason(), now);
        return detail(record);
    }

    @Transactional
    public ReturnDtos.Detail receive(UUID returnId, ReturnDtos.ReceiveRequest request, UUID actor) {
        ReturnRecord record = lock(returnId);
        if (record.status() == ReturnStatus.AWAITING_INSPECTION || record.status() == ReturnStatus.INSPECTED || record.status() == ReturnStatus.RESTOCKED) return detail(record);
        if (record.status() != ReturnStatus.REQUESTED && record.status() != ReturnStatus.IN_TRANSIT) throw conflict("RETURN_STATE_CONFLICT", "A devoluÃ§Ã£o nÃ£o pode ser recebida neste estado.");
        if (request.quantity() > record.quantity()) throw conflict("RETURN_QUANTITY_EXCEEDED", "A quantidade recebida excede a solicitada.");
        Instant now = Instant.now();
        returns.receive(record.id(), request.quantity(), actor, request.conditionNote(), now);
        returns.history(record.id(), record.status().name(), ReturnStatus.AWAITING_INSPECTION.name(), "RETURN_RECEIVED", actor, request.conditionNote(), now);
        return detail(returns.findByIdForUpdate(record.id()).orElseThrow());
    }

    @Transactional
    public ReturnDtos.Detail inspect(UUID returnId, ReturnDtos.InspectRequest request, UUID actor) {
        ReturnRecord record = lock(returnId);
        if (record.status() == ReturnStatus.INSPECTED || record.status() == ReturnStatus.RESTOCKED) return detail(record);
        if (record.status() != ReturnStatus.AWAITING_INSPECTION) throw conflict("RETURN_STATE_CONFLICT", "A devoluÃ§Ã£o ainda nÃ£o foi recebida para inspeÃ§Ã£o.");
        if (request.approvedQuantity() + request.rejectedQuantity() != record.receivedQuantity()) throw conflict("INSPECTION_QUANTITY_INVALID", "As quantidades aprovadas e rejeitadas devem totalizar o recebimento.");
        Instant now = Instant.now();
        returns.inspect(record.id(), request.approvedQuantity(), request.rejectedQuantity(), actor, request.reason(), now);
        returns.history(record.id(), record.status().name(), ReturnStatus.INSPECTED.name(), "RETURN_INSPECTED", actor, request.reason(), now);
        return detail(returns.findByIdForUpdate(record.id()).orElseThrow());
    }

    @Transactional
    public ReturnDtos.Detail restock(UUID returnId, ReturnDtos.RestockRequest request, UUID actor, String key,
            String correlation) {
        String operationKey = key == null || key.isBlank() ? "restock:" + digest(returnId + "|" + request.quantity()) : validateKey(key);
        ReturnRepository.RestockRecord previous = returns.findRestockByKey(operationKey).orElse(null);
        if (previous != null) return detail(lock(previous.returnId()));
        ReturnRecord record = lock(returnId);
        if (record.status() != ReturnStatus.INSPECTED && record.status() != ReturnStatus.RESTOCKED) throw conflict("RESTOCK_NOT_ALLOWED", "A reposiÃ§Ã£o exige inspeÃ§Ã£o concluÃ­da.");
        int already = returns.restockedQuantity(record.id());
        int remaining = record.approvedQuantity() - already;
        if (request.quantity() > remaining) throw conflict("RESTOCK_QUANTITY_EXCEEDED", "A quantidade excede a aprovada na inspeÃ§Ã£o.");
        String inventoryKey = "restock:" + record.id().toString().replace("-", "") + ":" + digest(operationKey).substring(0, 24);
        Instant now = Instant.now();
        inventory.adjust(record.presentationId(), request.quantity(), InventoryMovementType.RESTOCK,
                "ReposiÃ§Ã£o da devoluÃ§Ã£o " + record.id(), inventoryKey, actor, correlation);
        returns.insertRestock(new ReturnRepository.RestockRecord(UUID.randomUUID(), record.id(), record.presentationId(), request.quantity(), operationKey, inventoryKey, actor, now));
        int total = already + request.quantity();
        returns.markRestocked(record.id(), total, now);
        returns.history(record.id(), record.status().name(), total == record.approvedQuantity() ? ReturnStatus.RESTOCKED.name() : ReturnStatus.INSPECTED.name(), "RETURN_RESTOCKED", actor, "Quantidade: " + request.quantity(), now);
        return detail(returns.findByIdForUpdate(record.id()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnDtos.Summary> list(PageQuery page, ReturnStatus status) {
        List<ReturnRecord> records = returns.findPage(page, status);
        long total = returns.count(status);
        return new PageResponse<>(records.stream().map(this::summary).toList(), page.getPage(), page.getSize(), total,
                PageResponse.totalPages(total, page.getSize()), List.of(new SortOrder("requestedAt", SortOrder.Direction.DESC), new SortOrder("id", SortOrder.Direction.DESC)));
    }

    @Transactional(readOnly = true)
    public ReturnDtos.Detail get(UUID id) { return detail(returns.findById(id).orElseThrow(() -> new ApiRequestException(404, "RETURN_NOT_FOUND", "A devoluÃ§Ã£o nÃ£o foi encontrada."))); }

    private ReturnDtos.Detail detail(ReturnRecord record) { return new ReturnDtos.Detail(summary(record), returns.historyFor(record.id()), returns.restockedQuantity(record.id())); }
    private ReturnDtos.Summary summary(ReturnRecord record) { return new ReturnDtos.Summary(record.id(), record.orderId(), record.orderItemId(), record.presentationId(), record.quantity(), record.receivedQuantity(), record.approvedQuantity(), record.rejectedQuantity(), record.status(), record.reason(), record.requestedAt(), record.receivedAt(), record.inspectedAt()); }
    private ReturnRecord lock(UUID id) { return returns.findByIdForUpdate(id).orElseThrow(() -> new ApiRequestException(404, "RETURN_NOT_FOUND", "A devoluÃ§Ã£o nÃ£o foi encontrada.")); }
    private OrderRecord lockOrder(String publicNumber) { return orders.findByPublicNumber(publicNumber).flatMap(o -> orders.findByIdForUpdate(o.id())).orElseThrow(() -> new ApiRequestException(404, "ORDER_NOT_FOUND", "O pedido nÃ£o foi encontrado.")); }
    private int jdbcSumForItem(UUID itemId) { return returns.findByItem(itemId); }
    private String validateKey(String key) { if (!key.matches("[A-Za-z0-9._:-]{8,100}")) throw new ApiRequestException(422, "IDEMPOTENCY_KEY_INVALID", "Informe uma chave de idempotÃªncia vÃ¡lida."); return key; }
    private String digest(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private ApiRequestException conflict(String code, String message) { return new ApiRequestException(409, code, message); }
}
