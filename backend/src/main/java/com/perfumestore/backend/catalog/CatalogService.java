package com.perfumestore.backend.catalog;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.perfumestore.backend.config.CatalogImageProperties;
import com.perfumestore.backend.shared.api.ApiRequestException;
import com.perfumestore.backend.shared.api.MoneyResponse;
import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;
import com.perfumestore.backend.shared.api.SortOrder;
import com.perfumestore.backend.shared.inventory.InventoryAvailabilityReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogService.class);
    private static final String PNG = "image/png";
    private static final String JPEG = "image/jpeg";

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PresentationRepository presentations;
    private final ImageRepository images;
    private final ImageStorage imageStorage;
    private final CatalogImageProperties imageProperties;
    private final InventoryAvailabilityReader availability;

    public CatalogService(CategoryRepository categories, ProductRepository products,
            PresentationRepository presentations, ImageRepository images, ImageStorage imageStorage,
            CatalogImageProperties imageProperties, InventoryAvailabilityReader availability) {
        this.categories = categories;
        this.products = products;
        this.presentations = presentations;
        this.images = images;
        this.imageStorage = imageStorage;
        this.imageProperties = imageProperties;
        this.availability = availability;
    }

    @Transactional
    public CatalogDtos.CategoryResponse createCategory(CatalogDtos.CategoryRequest request) {
        String name = CatalogRules.text(request.name());
        String slug = CatalogRules.slug(request.slug());
        try {
            return categoryResponse(categories.insert(UUID.randomUUID(), name, slug,
                    request.active() == null || request.active(), Instant.now()));
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O slug da categoria já está cadastrado.");
        }
    }

    @Transactional
    public CatalogDtos.CategoryResponse updateCategory(UUID id, CatalogDtos.CategoryRequest request, long version) {
        requireCategory(id);
        try {
            CategoryRecord updated = categories.update(id, CatalogRules.text(request.name()),
                    CatalogRules.slug(request.slug()), request.active() == null || request.active(), version,
                    Instant.now()).orElseThrow(() -> versionConflict("categoria"));
            return categoryResponse(updated);
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O slug da categoria já está cadastrado.");
        }
    }

    public CatalogDtos.CategoryResponse category(UUID id) {
        return categoryResponse(requireCategory(id));
    }

    public PageResponse<CatalogDtos.CategoryResponse> categories(PageQuery query, boolean onlyActive) {
        SortOrder requested = sort(query, Map.of("name", "name", "createdAt", "created_at"), "name");
        List<CategoryRecord> records = categories.findPage(query.getPage(), query.getSize(), onlyActive,
                requested.property(), direction(requested));
        long total = categories.count(onlyActive);
        return new PageResponse<>(records.stream().map(this::categoryResponse).toList(), query.getPage(),
                query.getSize(), total, PageResponse.totalPages(total, query.getSize()),
                List.of(requested, new SortOrder("id", SortOrder.Direction.ASC)));
    }

    @Transactional
    public CatalogDtos.AdminProductDetail createProduct(CatalogDtos.ProductRequest request) {
        requireCategory(request.categoryId());
        boolean active = Boolean.TRUE.equals(request.active());
        if (active) {
            throw new ApiRequestException(409, "PRODUCT_NOT_PUBLISHABLE",
                    "Crie apresentações e uma imagem principal antes de publicar o produto.");
        }
        try {
            ProductRecord product = products.insert(UUID.randomUUID(), CatalogRules.slug(request.slug()),
                    CatalogRules.text(request.name()), CatalogRules.text(request.brand()),
                    CatalogRules.text(request.description()), request.categoryId(), false, Instant.now());
            return adminDetail(product);
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O slug do produto já está cadastrado.");
        }
    }

    @Transactional
    public CatalogDtos.AdminProductDetail updateProduct(UUID id, CatalogDtos.ProductUpdateRequest request) {
        ProductRecord current = requireProduct(id);
        requireCategory(request.categoryId());
        try {
            ProductRecord updated = products.update(id, CatalogRules.slug(request.slug()),
                    CatalogRules.text(request.name()), CatalogRules.text(request.brand()),
                    CatalogRules.text(request.description()), request.categoryId(), request.version(), Instant.now())
                    .orElseThrow(() -> versionConflict("produto"));
            return adminDetail(updated);
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O slug do produto já está cadastrado.");
        }
    }

    @Transactional
    public CatalogDtos.AdminProductDetail setProductActive(UUID id, CatalogDtos.StatusRequest request) {
        ProductRecord current = requireProduct(id);
        if (request.active()) {
            ensurePublishable(current);
        }
        ProductRecord updated = products.updateActive(id, request.active(), request.version(), Instant.now())
                .orElseThrow(() -> versionConflict("produto"));
        return adminDetail(updated);
    }

    public CatalogDtos.AdminProductDetail adminProduct(UUID id) {
        return adminDetail(requireProduct(id));
    }

    public PageResponse<CatalogDtos.AdminProductDetail> adminProducts(PageQuery query, String search, Boolean active) {
        SortOrder requested = sort(query, Map.of("name", "p.name", "createdAt", "p.created_at", "slug", "p.slug"),
                "name");
        List<ProductRecord> records = products.findAdminPage(query.getPage(), query.getSize(), cleanSearch(search),
                active, requested.property(), direction(requested));
        long total = products.countAdmin(cleanSearch(search), active);
        return new PageResponse<>(records.stream().map(this::adminDetail).toList(), query.getPage(), query.getSize(),
                total, PageResponse.totalPages(total, query.getSize()),
                List.of(requested, new SortOrder("id", SortOrder.Direction.ASC)));
    }

    public PageResponse<CatalogDtos.ProductSummary> publicProducts(PageQuery query, String search, String categorySlug) {
        SortOrder requested = sort(query, Map.of("name", "p.name", "createdAt", "p.created_at", "slug", "p.slug"),
                "name");
        String category = categorySlug == null || categorySlug.isBlank() ? null : CatalogRules.slug(categorySlug);
        List<ProductRecord> records = products.findPublicPage(query.getPage(), query.getSize(), cleanSearch(search),
                category, requested.property(), direction(requested));
        long total = products.countPublic(cleanSearch(search), category);
        Map<UUID, List<PresentationRecord>> pres = presentations.findByProducts(
                records.stream().map(ProductRecord::id).toList(), true);
        Map<UUID, List<ImageRecord>> imageMap = images.findByProducts(records.stream().map(ProductRecord::id).toList(), true);
        Map<UUID, Integer> available = availability.availableByPresentationIds(pres.values().stream()
                .flatMap(List::stream).map(PresentationRecord::id).toList());
        List<CatalogDtos.ProductSummary> items = records.stream()
                .map(record -> summary(record, pres.getOrDefault(record.id(), List.of()),
                        imageMap.getOrDefault(record.id(), List.of()), available))
                .toList();
        return new PageResponse<>(items, query.getPage(), query.getSize(), total,
                PageResponse.totalPages(total, query.getSize()),
                List.of(requested, new SortOrder("id", SortOrder.Direction.ASC)));
    }

    public CatalogDtos.ProductDetail publicProduct(String slug) {
        ProductRecord product = products.findPublicBySlug(CatalogRules.slug(slug))
                .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "O produto não foi encontrado."));
        return detail(product, presentations.findByProduct(product.id(), true), images.findByProduct(product.id(), true));
    }

    public List<CatalogDtos.PresentationResponse> publicPresentations(String slug) {
        CatalogDtos.ProductDetail product = publicProduct(slug);
        return product.presentations();
    }

    public List<CatalogDtos.ImageResponse> publicImages(String slug) {
        CatalogDtos.ProductDetail product = publicProduct(slug);
        return product.images();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CartPresentation> cartPresentation(UUID presentationId) {
        return presentations.findById(presentationId).flatMap(presentation -> products.findById(presentation.productId())
                .map(product -> new CartPresentation(presentation.id(), product.id(), product.name(), product.slug(),
                        presentation.sku(), presentation.volume(), presentation.unit(), presentation.price(),
                        presentation.currency(), presentation.active() && products.findPublicBySlug(product.slug()).isPresent())));
    }

    public record CartPresentation(UUID presentationId, UUID productId, String productName, String productSlug,
            String sku, BigDecimal volume, String unit, BigDecimal price, String currency, boolean published) { }

    @Transactional
    public CatalogDtos.PresentationResponse createPresentation(UUID productId,
            CatalogDtos.PresentationRequest request) {
        requireProduct(productId);
        BigDecimal volume = CatalogRules.scaleVolume(request.volume());
        BigDecimal price = CatalogRules.scaleMoney(request.price());
        try {
            return presentationResponses(List.of(presentations.insert(UUID.randomUUID(), productId, CatalogRules.text(request.sku()),
                    CatalogRules.sku(request.sku()), volume, "ML", price,
                    request.active() == null || request.active(), Instant.now()))).get(0);
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O SKU já está cadastrado.");
        }
    }

    @Transactional
    public CatalogDtos.PresentationResponse updatePresentation(UUID productId, UUID presentationId,
            CatalogDtos.PresentationRequest request) {
        ProductRecord product = requireProduct(productId);
        PresentationRecord current = presentations.findById(presentationId)
                .orElseThrow(() -> notFound("PRESENTATION_NOT_FOUND", "A apresentação não foi encontrada."));
        if (!productId.equals(current.productId())) {
            throw notFound("PRESENTATION_NOT_FOUND", "A apresentação não foi encontrada.");
        }
        if (!Boolean.TRUE.equals(request.active()) && product.active() && current.active()
                && presentations.countActiveByProduct(productId) <= 1) {
            throw new ApiRequestException(409, "PRODUCT_REQUIRES_PRESENTATION",
                    "Um produto publicado precisa manter pelo menos uma apresentação ativa.");
        }
        try {
            return presentationResponses(List.of(presentations.update(presentationId, productId, CatalogRules.text(request.sku()),
                    CatalogRules.sku(request.sku()), CatalogRules.scaleVolume(request.volume()),
                    CatalogRules.scaleMoney(request.price()), request.active() == null || request.active(),
                    request.version(), Instant.now()).orElseThrow(() -> versionConflict("apresentação")))).get(0);
        } catch (DuplicateKeyException exception) {
            throw CatalogRules.conflict("O SKU já está cadastrado.");
        }
    }

    public List<CatalogDtos.PresentationResponse> adminPresentations(UUID productId) {
        requireProduct(productId);
        return presentationResponses(presentations.findByProduct(productId, false));
    }

    @Transactional
    public CatalogDtos.AdminImageResponse uploadImage(UUID productId, MultipartFile file, String altText) {
        ProductRecord product = requireProduct(productId);
        validateAltText(altText);
        byte[] bytes = readAndValidateImage(file);
        ImageInfo info = inspect(bytes, file.getContentType());
        if (images.countActiveByProduct(productId) >= imageProperties.getMaxImagesPerProduct()) {
            throw new ApiRequestException(413, "IMAGE_LIMIT_EXCEEDED", "O produto atingiu o limite de imagens.");
        }
        ImageStorage.StoredImage stored = null;
        try {
            stored = imageStorage.store(bytes, info.contentType(), info.extension());
            int position = (int) images.countActiveByProduct(productId);
            boolean primary = position == 0;
            ImageRecord record = images.insert(UUID.randomUUID(), productId, stored.storageKey(), info.contentType(),
                    stored.byteSize(), info.width(), info.height(), CatalogRules.text(altText), position, primary,
                    Instant.now());
            return adminImageResponse(record, product.slug());
        } catch (IOException | RuntimeException exception) {
            if (stored != null) {
                try {
                    imageStorage.delete(stored.storageKey());
                } catch (IOException cleanupException) {
                    LOGGER.error("Falha ao limpar arquivo de imagem órfão", cleanupException);
                }
            }
            if (exception instanceof ApiRequestException api) {
                throw api;
            }
            throw new ApiRequestException(500, "IMAGE_STORAGE_ERROR",
                    "Não foi possível armazenar a imagem.");
        }
    }

    public List<CatalogDtos.AdminImageResponse> adminImages(UUID productId) {
        ProductRecord product = requireProduct(productId);
        return images.findByProduct(productId, true).stream().map(image -> adminImageResponse(image, product.slug())).toList();
    }

    @Transactional
    public void reorderImages(UUID productId, CatalogDtos.ImageOrderRequest request) {
        requireProduct(productId);
        List<ImageRecord> current = images.findByProduct(productId, true);
        if (current.size() != request.imageIds().size()
                || !current.stream().map(ImageRecord::id).collect(Collectors.toSet())
                        .equals(request.imageIds().stream().collect(Collectors.toSet()))) {
            throw CatalogRules.conflict("A ordenação deve conter exatamente as imagens ativas deste produto.");
        }
        if (request.imageIds().size() != request.imageIds().stream().distinct().count()) {
            throw CatalogRules.conflict("A ordenação não pode repetir imagens.");
        }
        images.reorder(productId, request.imageIds(), Instant.now());
    }

    @Transactional
    public void setPrimaryImage(UUID productId, CatalogDtos.ImagePrimaryRequest request) {
        requireProduct(productId);
        if (images.findByProductAndId(productId, request.imageId()).isEmpty()) {
            throw notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada neste produto.");
        }
        images.setPrimary(productId, request.imageId(), Instant.now());
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        ProductRecord product = requireProduct(productId);
        ImageRecord image = images.findByProductAndId(productId, imageId)
                .orElseThrow(() -> notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada neste produto."));
        if (product.active() && image.primary()) {
            throw new ApiRequestException(409, "PRODUCT_REQUIRES_PRIMARY_IMAGE",
                    "Desative o produto ou defina outra imagem principal antes de remover esta imagem.");
        }
        images.softDelete(productId, imageId, Instant.now());
    }

    public Resource imageContent(UUID productId, UUID imageId, boolean requirePublished) {
        ProductRecord product = requireProduct(productId);
        if (requirePublished && products.findPublicBySlug(product.slug()).isEmpty()) {
            throw notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada.");
        }
        ImageRecord image = images.findByProductAndId(productId, imageId)
                .orElseThrow(() -> notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada."));
        try {
            return imageStorage.load(image.storageKey());
        } catch (IOException exception) {
            throw notFound("IMAGE_NOT_FOUND", "A imagem não está disponível.");
        }
    }

    public ImageRecord imageRecord(UUID productId, UUID imageId, boolean requirePublished) {
        ProductRecord product = requireProduct(productId);
        if (requirePublished && products.findPublicBySlug(product.slug()).isEmpty()) {
            throw notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada.");
        }
        return images.findByProductAndId(productId, imageId)
                .orElseThrow(() -> notFound("IMAGE_NOT_FOUND", "A imagem não foi encontrada."));
    }

    private void ensurePublishable(ProductRecord product) {
        if (!product.categoryActive() || presentations.countActiveByProduct(product.id()) == 0
                || images.countPrimaryByProduct(product.id()) != 1) {
            throw new ApiRequestException(409, "PRODUCT_NOT_PUBLISHABLE",
                    "O produto precisa de categoria ativa, apresentação ativa e uma imagem principal.");
        }
    }

    private CatalogDtos.AdminProductDetail adminDetail(ProductRecord product) {
        List<PresentationRecord> pres = presentations.findByProduct(product.id(), false);
        List<ImageRecord> imageList = images.findByProduct(product.id(), true);
        boolean publishable = product.categoryActive() && pres.stream().anyMatch(PresentationRecord::active)
                && imageList.stream().anyMatch(ImageRecord::primary);
        return new CatalogDtos.AdminProductDetail(product.id(), product.slug(), product.name(), product.brand(),
                product.description(), categoryResponse(product), product.active(), publishable, product.version(),
                product.createdAt(), product.updatedAt(), presentationResponses(pres),
                imageList.stream().map(image -> adminImageResponse(image, product.slug())).toList());
    }

    private CatalogDtos.ProductDetail detail(ProductRecord product, List<PresentationRecord> pres,
            List<ImageRecord> imageList) {
        return new CatalogDtos.ProductDetail(product.id(), product.slug(), product.name(), product.brand(),
                product.description(), categoryResponse(product), product.active(), true, product.version(),
                product.createdAt(), product.updatedAt(), presentationResponses(pres),
                imageList.stream().map(image -> imageResponse(image, product.slug())).toList());
    }

    private CatalogDtos.ProductSummary summary(ProductRecord product, List<PresentationRecord> pres,
            List<ImageRecord> imageList, Map<UUID, Integer> available) {
        return new CatalogDtos.ProductSummary(product.id(), product.slug(), product.name(), product.brand(),
                categoryResponse(product), product.active(), product.published(!pres.isEmpty(),
                        imageList.stream().anyMatch(ImageRecord::primary)), product.version(),
                presentationResponses(pres, available),
                imageList.stream().map(image -> imageResponse(image, product.slug())).toList());
    }

    private CatalogDtos.CategoryResponse categoryResponse(CategoryRecord category) {
        return new CatalogDtos.CategoryResponse(category.id(), category.name(), category.slug(), category.active(),
                category.version(), category.createdAt(), category.updatedAt());
    }

    private CatalogDtos.CategoryResponse categoryResponse(ProductRecord product) {
        return new CatalogDtos.CategoryResponse(product.categoryId(), product.categoryName(), product.categorySlug(),
                product.categoryActive(), product.categoryVersion(), product.categoryCreatedAt(),
                product.categoryUpdatedAt());
    }

    private List<CatalogDtos.PresentationResponse> presentationResponses(List<PresentationRecord> records) {
        Map<UUID, Integer> available = availability.availableByPresentationIds(
                records.stream().map(PresentationRecord::id).toList());
        return presentationResponses(records, available);
    }

    private List<CatalogDtos.PresentationResponse> presentationResponses(List<PresentationRecord> records,
            Map<UUID, Integer> available) {
        return records.stream().map(presentation -> {
            int quantity = available.getOrDefault(presentation.id(), 0);
            return new CatalogDtos.PresentationResponse(presentation.id(), presentation.productId(), presentation.sku(),
                    presentation.volume(), presentation.unit(), new MoneyResponse(presentation.price(), "BRL"),
                    presentation.currency(), presentation.active(), presentation.version(), presentation.createdAt(),
                    presentation.updatedAt(), new CatalogDtos.AvailabilityResponse(quantity,
                            presentation.active() && quantity > 0));
        }).toList();
    }

    private CatalogDtos.ImageResponse imageResponse(ImageRecord image, String slug) {
        return new CatalogDtos.ImageResponse(image.id(), imageUrl(slug, image.id()), image.altText(), image.position(),
                image.primary(), image.contentType(), image.width(), image.height());
    }

    private CatalogDtos.AdminImageResponse adminImageResponse(ImageRecord image, String slug) {
        return new CatalogDtos.AdminImageResponse(image.id(), "/api/v1/admin/products/" + image.productId()
                + "/images/" + image.id() + "/content", image.altText(), image.position(), image.primary(),
                image.contentType(), image.byteSize(), image.width(), image.height(), image.createdAt(), image.updatedAt());
    }

    private String imageUrl(String slug, UUID imageId) {
        return "/api/v1/catalog/products/" + slug + "/images/" + imageId + "/content";
    }

    private byte[] readAndValidateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiRequestException(415, "UNSUPPORTED_IMAGE", "Envie uma imagem PNG ou JPEG válida.");
        }
        if (file.getSize() > imageProperties.getMaxBytes()) {
            throw new ApiRequestException(413, "IMAGE_TOO_LARGE", "A imagem excede o tamanho máximo permitido.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > imageProperties.getMaxBytes()) {
                throw new ApiRequestException(413, "IMAGE_TOO_LARGE", "A imagem excede o tamanho máximo permitido.");
            }
            return bytes;
        } catch (IOException exception) {
            throw new ApiRequestException(400, "INVALID_IMAGE", "Não foi possível ler a imagem enviada.");
        }
    }

    private ImageInfo inspect(byte[] bytes, String declaredContentType) {
        String actual;
        String extension;
        if (isPng(bytes)) {
            actual = PNG;
            extension = ".png";
        } else if (isJpeg(bytes)) {
            actual = JPEG;
            extension = ".jpg";
        } else {
            throw new ApiRequestException(415, "UNSUPPORTED_IMAGE", "Aceitamos somente imagens PNG ou JPEG.");
        }
        if (declaredContentType != null && !declaredContentType.isBlank()
                && !actual.equalsIgnoreCase(declaredContentType)) {
            throw new ApiRequestException(415, "IMAGE_CONTENT_TYPE_MISMATCH",
                    "O conteúdo da imagem não corresponde ao tipo declarado.");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || (long) image.getWidth() * image.getHeight() > imageProperties.getMaxPixels()) {
                throw new ApiRequestException(413, "IMAGE_DIMENSIONS_EXCEEDED",
                        "As dimensões da imagem excedem o limite permitido.");
            }
            return new ImageInfo(actual, extension, image.getWidth(), image.getHeight());
        } catch (IOException exception) {
            throw new ApiRequestException(415, "INVALID_IMAGE", "O arquivo de imagem é inválido.");
        }
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
    }

    private void validateAltText(String altText) {
        if (altText == null || CatalogRules.text(altText).length() < 2 || CatalogRules.text(altText).length() > 180) {
            throw new ApiRequestException(422, "VALIDATION_ERROR", "Informe um texto alternativo entre 2 e 180 caracteres.");
        }
    }

    private ProductRecord requireProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "O produto não foi encontrado."));
    }

    private CategoryRecord requireCategory(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> notFound("CATEGORY_NOT_FOUND", "A categoria não foi encontrada."));
    }

    private SortOrder sort(PageQuery query, Map<String, String> allowed, String defaultField) {
        String raw = query.getSort();
        if (raw == null || raw.isBlank() || "id,asc".equals(raw)) {
            return new SortOrder(allowed.getOrDefault(defaultField, defaultField), SortOrder.Direction.ASC);
        }
        String[] pieces = raw.split(",", -1);
        if (pieces.length != 2 || !allowed.containsKey(pieces[0].trim())) {
            throw new ApiRequestException(400, "INVALID_SORT", "A ordenação solicitada não é permitida.");
        }
        try {
            return new SortOrder(allowed.get(pieces[0].trim()), SortOrder.Direction.valueOf(pieces[1].trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new ApiRequestException(400, "INVALID_SORT", "A ordenação solicitada não é permitida.");
        }
    }

    private String direction(SortOrder sort) {
        return sort.direction() == SortOrder.Direction.DESC ? "desc" : "asc";
    }

    private String cleanSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }

    private ApiRequestException versionConflict(String resource) {
        return new ApiRequestException(409, "CONCURRENT_UPDATE", "O " + resource
                + " foi alterado por outra operação. Consulte novamente antes de salvar.");
    }

    private ApiRequestException notFound(String code, String message) {
        return new ApiRequestException(404, code, message);
    }

    private record ImageInfo(String contentType, String extension, int width, int height) { }
}
