package com.perfumestore.backend.address;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.api.SortParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "createdAt", "created_at", "updatedAt", "updated_at", "label", "label", "city", "city");

    private final AddressRepository addresses;

    public AddressService(AddressRepository addresses) {
        this.addresses = addresses;
    }

    @Transactional
    public AddressDtos.AddressResponse create(UUID userId, AddressDtos.AddressRequest request) {
        Normalized normalized = normalize(request);
        AddressRecord created = addresses.insert(UUID.randomUUID(), userId, normalized.recipientName(),
                normalized.label(), normalized.postalCode(), normalized.street(), normalized.number(),
                normalized.complement(), normalized.neighborhood(), normalized.city(), normalized.state(),
                normalized.reference(), java.time.Instant.now());
        return response(created);
    }

    @Transactional(readOnly = true)
    public PageResponse<AddressDtos.AddressResponse> list(UUID userId, PageQuery query) {
        SortOrder requested = SortParser.parse(query.getSort(), SORTS, "createdAt");
        List<AddressRecord> records = addresses.findPageForUser(userId, query.getPage(), query.getSize(),
                requested.property(), requested.direction() == SortOrder.Direction.DESC ? "desc" : "asc");
        long total = addresses.countForUser(userId);
        return new PageResponse<>(records.stream().map(this::response).toList(), query.getPage(), query.getSize(),
                total, PageResponse.totalPages(total, query.getSize()),
                SortParser.withTieBreaker(requested, "id"));
    }

    @Transactional(readOnly = true)
    public AddressDtos.AddressResponse get(UUID userId, UUID addressId) {
        return response(requireOwnedRecord(userId, addressId));
    }

    @Transactional
    public AddressDtos.AddressResponse update(UUID userId, UUID addressId, AddressDtos.AddressUpdateRequest request) {
        Normalized normalized = normalize(request);
        requireOwnedRecord(userId, addressId);
        AddressRecord updated = addresses.update(userId, addressId, normalized.recipientName(), normalized.label(),
                normalized.postalCode(), normalized.street(), normalized.number(), normalized.complement(),
                normalized.neighborhood(), normalized.city(), normalized.state(), normalized.reference(),
                request.version(), java.time.Instant.now()).orElseThrow(() -> new ApiRequestException(409,
                        "CONCURRENT_UPDATE", "O endereço foi alterado. Consulte a versão atual e tente novamente."));
        return response(updated);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        requireOwnedRecord(userId, addressId);
        if (addresses.softDelete(userId, addressId, java.time.Instant.now()) == 0) {
            throw new ApiRequestException(409, "CONCURRENT_UPDATE", "O endereço foi alterado. Tente novamente.");
        }
    }

    @Transactional(readOnly = true)
    public AddressDtos.AddressData requireOwned(UUID userId, UUID addressId) {
        AddressRecord record = requireOwnedRecord(userId, addressId);
        return new AddressDtos.AddressData(record.id(), record.userId(), record.postalCode(), record.city(),
                record.state());
    }

    @Transactional(readOnly = true)
    public AddressDtos.AddressSnapshot requireOwnedSnapshot(UUID userId, UUID addressId) {
        AddressRecord record = requireOwnedRecord(userId, addressId);
        return new AddressDtos.AddressSnapshot(record.id(), record.userId(), record.recipientName(), record.label(),
                record.postalCode(), record.street(), record.number(), record.complement(), record.neighborhood(),
                record.city(), record.state(), record.reference());
    }

    private AddressRecord requireOwnedRecord(UUID userId, UUID addressId) {
        return addresses.findByIdForUser(userId, addressId).orElseThrow(() -> new ApiRequestException(404,
                "ADDRESS_NOT_FOUND", "O endereço não foi encontrado."));
    }

    private Normalized normalize(AddressDtos.AddressRequest request) {
        return normalize(request.recipientName(), request.label(), request.postalCode(), request.street(),
                request.number(), request.complement(), request.neighborhood(), request.city(), request.state(),
                request.reference());
    }

    private Normalized normalize(AddressDtos.AddressUpdateRequest request) {
        return normalize(request.recipientName(), request.label(), request.postalCode(), request.street(),
                request.number(), request.complement(), request.neighborhood(), request.city(), request.state(),
                request.reference());
    }

    private Normalized normalize(String recipientName, String label, String postalCode, String street, String number,
            String complement, String neighborhood, String city, String state, String reference) {
        String normalizedPostalCode = postalCode == null ? null : postalCode.replace("-", "").trim();
        if (normalizedPostalCode == null || !normalizedPostalCode.matches("[0-9]{8}")) {
            throw new ApiRequestException(422, "INVALID_POSTAL_CODE", "Informe um CEP válido.");
        }
        String normalizedState = state == null ? null : state.trim().toUpperCase(Locale.ROOT);
        if (normalizedState == null || !AddressRules.isBrazilianState(normalizedState)) {
            throw new ApiRequestException(422, "INVALID_STATE", "Informe uma UF brasileira válida.");
        }
        return new Normalized(cleanRequired(recipientName), cleanOptional(label), normalizedPostalCode,
                cleanRequired(street), cleanRequired(number), cleanOptional(complement), cleanRequired(neighborhood),
                cleanRequired(city), normalizedState, cleanOptional(reference));
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AddressDtos.AddressResponse response(AddressRecord record) {
        return new AddressDtos.AddressResponse(record.id(), record.recipientName(), record.label(), record.postalCode(),
                record.street(), record.number(), record.complement(), record.neighborhood(), record.city(),
                record.state(), record.reference(), record.version(), record.createdAt(), record.updatedAt());
    }

    private record Normalized(String recipientName, String label, String postalCode, String street, String number,
            String complement, String neighborhood, String city, String state, String reference) {
    }
}
