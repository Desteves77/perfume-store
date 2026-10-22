package com.perfumestore.backend.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() { }

    public record CategoryRequest(
            @NotBlank(message = "Informe o nome da categoria.")
            @Size(min = 2, max = 120, message = "O nome da categoria deve ter entre 2 e 120 caracteres.")
            String name,
            @NotBlank(message = "Informe o slug da categoria.")
            @Size(max = 120, message = "O slug da categoria é muito longo.")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use apenas letras minúsculas, números e hífens.")
            String slug,
            Boolean active) { }

    public record CategoryUpdateRequest(
            @NotBlank(message = "Informe o nome da categoria.")
            @Size(min = 2, max = 120, message = "O nome da categoria deve ter entre 2 e 120 caracteres.")
            String name,
            @NotBlank(message = "Informe o slug da categoria.")
            @Size(max = 120, message = "O slug da categoria é muito longo.")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use apenas letras minúsculas, números e hífens.")
            String slug,
            @NotNull(message = "Informe o estado da categoria.") Boolean active,
            @NotNull(message = "Informe a versão atual da categoria.") Long version) { }

    public record CategoryResponse(UUID id, String name, String slug, boolean active, long version,
            Instant createdAt, Instant updatedAt) { }

    public record ProductRequest(
            @NotBlank(message = "Informe o nome do produto.")
            @Size(min = 2, max = 180, message = "O nome deve ter entre 2 e 180 caracteres.")
            String name,
            @NotBlank(message = "Informe a marca do produto.")
            @Size(min = 2, max = 120, message = "A marca deve ter entre 2 e 120 caracteres.")
            String brand,
            @NotBlank(message = "Informe a descrição do produto.")
            @Size(min = 1, max = 5000, message = "A descrição deve ter até 5000 caracteres.")
            String description,
            @NotNull(message = "Informe a categoria do produto.")
            UUID categoryId,
            @NotBlank(message = "Informe o slug do produto.")
            @Size(max = 180, message = "O slug do produto é muito longo.")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use apenas letras minúsculas, números e hífens.")
            String slug,
            Boolean active) { }

    public record ProductUpdateRequest(
            @NotBlank(message = "Informe o nome do produto.")
            @Size(min = 2, max = 180, message = "O nome deve ter entre 2 e 180 caracteres.")
            String name,
            @NotBlank(message = "Informe a marca do produto.")
            @Size(min = 2, max = 120, message = "A marca deve ter entre 2 e 120 caracteres.")
            String brand,
            @NotBlank(message = "Informe a descrição do produto.")
            @Size(min = 1, max = 5000, message = "A descrição deve ter até 5000 caracteres.")
            String description,
            @NotNull(message = "Informe a categoria do produto.")
            UUID categoryId,
            @NotBlank(message = "Informe o slug do produto.")
            @Size(max = 180, message = "O slug do produto é muito longo.")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use apenas letras minúsculas, números e hífens.")
            String slug,
            @NotNull(message = "Informe a versão atual do produto.")
            Long version) { }

    public record PresentationRequest(
            @NotBlank(message = "Informe o SKU.")
            @Size(max = 80, message = "O SKU é muito longo.")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*", message = "Informe um SKU válido.")
            String sku,
            @NotNull(message = "Informe o volume.")
            @DecimalMin(value = "0.01", message = "O volume deve ser positivo.")
            @Digits(integer = 6, fraction = 2, message = "O volume deve ter no máximo duas casas decimais.")
            BigDecimal volume,
            @NotNull(message = "Informe o preço.")
            @DecimalMin(value = "0.01", message = "O preço deve ser positivo.")
            @Digits(integer = 10, fraction = 2, message = "O preço deve ter no máximo duas casas decimais.")
            BigDecimal price,
            Boolean active,
            Long version) { }

    public record PresentationUpdateRequest(
            @NotBlank(message = "Informe o SKU.")
            @Size(max = 80, message = "O SKU é muito longo.")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*", message = "Informe um SKU válido.")
            String sku,
            @NotNull(message = "Informe o volume.")
            @DecimalMin(value = "0.01", message = "O volume deve ser positivo.")
            @Digits(integer = 6, fraction = 2, message = "O volume deve ter no máximo duas casas decimais.")
            BigDecimal volume,
            @NotNull(message = "Informe o preço.")
            @DecimalMin(value = "0.01", message = "O preço deve ser positivo.")
            @Digits(integer = 10, fraction = 2, message = "O preço deve ter no máximo duas casas decimais.")
            BigDecimal price,
            @NotNull(message = "Informe o estado.") Boolean active,
            @NotNull(message = "Informe a versão atual da apresentação.") Long version) { }

    public record PresentationResponse(UUID id, UUID productId, String sku, BigDecimal volume, String unit,
            MoneyResponse price, String currency, boolean active, long version, Instant createdAt,
            Instant updatedAt, AvailabilityResponse availability) { }

    public record AvailabilityResponse(int availableQuantity, boolean availableForPurchase) { }

    public record ImageResponse(UUID id, String url, String altText, int position, boolean primary,
            String contentType, Integer width, Integer height) { }

    public record AdminImageResponse(UUID id, String url, String altText, int position, boolean primary,
            String contentType, long byteSize, int width, int height, Instant createdAt, Instant updatedAt) { }

    public record ProductSummary(UUID id, String slug, String name, String brand, CategoryResponse category,
            boolean active, boolean published, long version, List<PresentationResponse> presentations,
            List<ImageResponse> images) { }

    public record ProductDetail(UUID id, String slug, String name, String brand, String description,
            CategoryResponse category, boolean active, boolean published, long version,
            Instant createdAt, Instant updatedAt, List<PresentationResponse> presentations,
            List<ImageResponse> images) { }

    public record AdminProductDetail(UUID id, String slug, String name, String brand, String description,
            CategoryResponse category, boolean active, boolean publishable, long version,
            Instant createdAt, Instant updatedAt, List<PresentationResponse> presentations,
            List<AdminImageResponse> images) { }

    public record ImageOrderRequest(
            @NotNull(message = "Informe a lista de imagens.")
            @Size(min = 1, max = 8, message = "Informe entre 1 e 8 imagens.")
            List<@NotNull(message = "A imagem é obrigatória.") UUID> imageIds) { }

    public record ImagePrimaryRequest(
            @NotNull(message = "Informe a imagem principal.") UUID imageId) { }

    public record StatusRequest(
            @NotNull(message = "Informe o estado.") Boolean active,
            @NotNull(message = "Informe a versão atual do recurso.") Long version) { }

    public record ProductPage(PageResponse<ProductSummary> page) { }
}
