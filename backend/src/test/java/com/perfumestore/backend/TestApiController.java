package com.perfumestore.backend;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.api.SortParser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class TestApiController {

    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "id", "id",
            "name", "name",
            "price", "price");

    private static final List<TestItem> ITEMS = List.of(
            new TestItem("item-1", "Âmbar", new BigDecimal("29.90")),
            new TestItem("item-2", "Floral", new BigDecimal("99.90")),
            new TestItem("item-3", "Cítrico", new BigDecimal("149.90")));

    @PostMapping("/test-api/validation")
    ValidationResponse validate(@Valid @RequestBody ValidationRequest request) {
        return new ValidationResponse(new MoneyResponse(request.amount(), "BRL"),
                Instant.parse("2026-10-10T12:00:00Z"), LocalDate.of(2026, 10, 10));
    }

    @GetMapping("/test-api/page")
    PageResponse<TestItem> page(@Valid @ModelAttribute PageQuery query) {
        SortOrder requested = SortParser.parse(query.getSort(), ALLOWED_SORTS, "id");
        List<SortOrder> sort = SortParser.withTieBreaker(requested, "id");
        Comparator<TestItem> comparator = comparator(requested);
        List<TestItem> ordered = ITEMS.stream().sorted(comparator.thenComparing(TestItem::id)).toList();
        int from = Math.min(query.getPage() * query.getSize(), ordered.size());
        int to = Math.min(from + query.getSize(), ordered.size());
        return new PageResponse<>(ordered.subList(from, to), query.getPage(), query.getSize(), ordered.size(),
                PageResponse.totalPages(ordered.size(), query.getSize()), sort);
    }

    @GetMapping("/test-api/path/{id}")
    String path(@PathVariable @Positive long id) {
        return "item-" + id;
    }

    @GetMapping("/test-api/protected")
    ResponseEntity<Void> protectedResource() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-api/protected/write")
    ResponseEntity<Void> protectedWrite() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test-api/admin")
    ResponseEntity<Void> adminResource() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test-api/internal-error")
    String internalError() {
        throw new IllegalStateException("internal-marker-for-server-log-only");
    }

    private Comparator<TestItem> comparator(SortOrder order) {
        Comparator<TestItem> comparator = switch (order.property()) {
            case "name" -> Comparator.comparing(TestItem::name);
            case "price" -> Comparator.comparing(TestItem::price);
            default -> Comparator.comparing(TestItem::id);
        };
        return order.direction() == SortOrder.Direction.DESC ? comparator.reversed() : comparator;
    }

    public record ValidationRequest(
            @NotBlank(message = "Informe o nome.") String name,
            @NotNull(message = "Informe os dados adicionais.") @Valid NestedRequest nested,
            @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.01", message = "Informe um valor positivo.") BigDecimal amount) {
    }

    public record NestedRequest(@NotBlank(message = "Informe o rótulo.") String label) {
    }

    public record ValidationResponse(MoneyResponse money, Instant createdAt, LocalDate civilDate) {
    }

    public record TestItem(String id, String name, BigDecimal price) {
    }
}
