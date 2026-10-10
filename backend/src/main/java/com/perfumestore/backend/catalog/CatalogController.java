package com.perfumestore.backend.catalog;

import java.util.List;
import java.util.UUID;

import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public PageResponse<CatalogDtos.CategoryResponse> categories(@Valid @ModelAttribute PageQuery page) {
        return catalog.categories(page, true);
    }

    @GetMapping("/products")
    public PageResponse<CatalogDtos.ProductSummary> products(@Valid @ModelAttribute PageQuery page,
            @RequestParam(required = false) String q, @RequestParam(required = false) String category) {
        return catalog.publicProducts(page, q, category);
    }

    @GetMapping("/products/{slug}")
    public CatalogDtos.ProductDetail product(@PathVariable String slug) {
        return catalog.publicProduct(slug);
    }

    @GetMapping("/products/{slug}/presentations")
    public List<CatalogDtos.PresentationResponse> presentations(@PathVariable String slug) {
        return catalog.publicPresentations(slug);
    }

    @GetMapping("/products/{slug}/images")
    public List<CatalogDtos.ImageResponse> images(@PathVariable String slug) {
        return catalog.publicImages(slug);
    }

    @GetMapping("/products/{slug}/images/{imageId}/content")
    public ResponseEntity<Resource> imageContent(@PathVariable String slug, @PathVariable UUID imageId) {
        CatalogDtos.ProductDetail product = catalog.publicProduct(slug);
        CatalogDtos.ImageResponse image = product.images().stream()
                .filter(item -> item.id().equals(imageId)).findFirst()
                .orElseThrow(() -> new com.perfumestore.backend.shared.api.ApiRequestException(404,
                        "IMAGE_NOT_FOUND", "A imagem não foi encontrada."));
        Resource resource = catalog.imageContent(product.id(), imageId, true);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
