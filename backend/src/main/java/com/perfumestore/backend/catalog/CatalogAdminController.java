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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogAdminController {

    private final CatalogService catalog;

    public CatalogAdminController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public PageResponse<CatalogDtos.CategoryResponse> categories(@Valid @ModelAttribute PageQuery page) {
        return catalog.categories(page, false);
    }

    @PostMapping("/categories")
    public ResponseEntity<CatalogDtos.CategoryResponse> createCategory(
            @Valid @RequestBody CatalogDtos.CategoryRequest request) {
        return ResponseEntity.status(201).body(catalog.createCategory(request));
    }

    @PatchMapping("/categories/{id}")
    public CatalogDtos.CategoryResponse updateCategory(@PathVariable UUID id,
            @Valid @RequestBody CatalogDtos.CategoryUpdateRequest request) {
        return catalog.updateCategory(id,
                new CatalogDtos.CategoryRequest(request.name(), request.slug(), request.active()), request.version());
    }

    @GetMapping("/categories/{id}")
    public CatalogDtos.CategoryResponse category(@PathVariable UUID id) {
        return catalog.category(id);
    }

    @GetMapping("/products")
    public PageResponse<CatalogDtos.AdminProductDetail> products(@Valid @ModelAttribute PageQuery page,
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return catalog.adminProducts(page, q, active);
    }

    @PostMapping("/products")
    public ResponseEntity<CatalogDtos.AdminProductDetail> createProduct(
            @Valid @RequestBody CatalogDtos.ProductRequest request) {
        return ResponseEntity.status(201).body(catalog.createProduct(request));
    }

    @GetMapping("/products/{id}")
    public CatalogDtos.AdminProductDetail product(@PathVariable UUID id) {
        return catalog.adminProduct(id);
    }

    @PatchMapping("/products/{id}")
    public CatalogDtos.AdminProductDetail updateProduct(@PathVariable UUID id,
            @Valid @RequestBody CatalogDtos.ProductUpdateRequest request) {
        return catalog.updateProduct(id, request);
    }

    @PutMapping("/products/{id}/status")
    public CatalogDtos.AdminProductDetail setProductStatus(@PathVariable UUID id,
            @Valid @RequestBody CatalogDtos.StatusRequest request) {
        return catalog.setProductActive(id, request);
    }

    @PostMapping("/products/{productId}/presentations")
    public ResponseEntity<CatalogDtos.PresentationResponse> createPresentation(@PathVariable UUID productId,
            @Valid @RequestBody CatalogDtos.PresentationRequest request) {
        return ResponseEntity.status(201).body(catalog.createPresentation(productId, request));
    }

    @GetMapping("/products/{productId}/presentations")
    public List<CatalogDtos.PresentationResponse> presentations(@PathVariable UUID productId) {
        return catalog.adminPresentations(productId);
    }

    @PatchMapping("/products/{productId}/presentations/{presentationId}")
    public CatalogDtos.PresentationResponse updatePresentation(@PathVariable UUID productId,
            @PathVariable UUID presentationId, @Valid @RequestBody CatalogDtos.PresentationUpdateRequest request) {
        return catalog.updatePresentation(productId, presentationId,
                new CatalogDtos.PresentationRequest(request.sku(), request.volume(), request.price(), request.active(),
                        request.version()));
    }

    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CatalogDtos.AdminImageResponse> uploadImage(@PathVariable UUID productId,
            @RequestParam("file") MultipartFile file, @RequestParam("altText") String altText) {
        return ResponseEntity.status(201).body(catalog.uploadImage(productId, file, altText));
    }

    @GetMapping("/products/{productId}/images")
    public List<CatalogDtos.AdminImageResponse> images(@PathVariable UUID productId) {
        return catalog.adminImages(productId);
    }

    @PostMapping("/products/{productId}/images/reorder")
    public ResponseEntity<Void> reorderImages(@PathVariable UUID productId,
            @Valid @RequestBody CatalogDtos.ImageOrderRequest request) {
        catalog.reorderImages(productId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/products/{productId}/images/primary")
    public ResponseEntity<Void> setPrimaryImage(@PathVariable UUID productId,
            @Valid @RequestBody CatalogDtos.ImagePrimaryRequest request) {
        catalog.setPrimaryImage(productId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID productId, @PathVariable UUID imageId) {
        catalog.deleteImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/{productId}/images/{imageId}/content")
    public ResponseEntity<Resource> imageContent(@PathVariable UUID productId, @PathVariable UUID imageId) {
        ImageRecord image = catalog.imageRecord(productId, imageId, false);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(catalog.imageContent(productId, imageId, false));
    }
}
