package com.perfumestore.backend;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void appliesCatalogMigrationAndKeepsCommerceTablesOutOfThisPhase() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from perfumestore.flyway_schema_history where version = '3' and success = true",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'perfumestore' and table_name in ('categories', 'products', 'product_presentations', 'product_images')",
                Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'perfumestore' and table_name in ('stock', 'carts', 'orders')",
                Integer.class)).isEqualTo(0);
    }

    @Test
    void adminPublishesCatalogAndPublicApiHidesInactiveResources() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode category = json(mockMvc.perform(post("/api/v1/admin/categories")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Perfumes de teste %s","slug":"perfumes-%s","active":true}
                        """.formatted(suffix, suffix)))
                .andExpect(status().isCreated()).andReturn());
        String categoryId = category.get("id").asText();

        JsonNode product = json(mockMvc.perform(post("/api/v1/admin/products")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Perfume de teste %s","brand":"Marca Teste","description":"Descrição sintética para teste.","categoryId":"%s","slug":"perfume-%s","active":false}
                        """.formatted(suffix, categoryId, suffix)))
                .andExpect(status().isCreated()).andReturn());
        String productId = product.get("id").asText();

        mockMvc.perform(post("/api/v1/admin/products/{id}/presentations", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"TEST-%s-50\",\"volume\":50,\"price\":199.90,\"active\":true}".formatted(suffix)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price.amount").value("199.90"));
        mockMvc.perform(post("/api/v1/admin/products/{id}/presentations", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"TEST-%s-100\",\"volume\":100,\"price\":299.90,\"active\":true}".formatted(suffix)))
                .andExpect(status().isCreated());

        MockMultipartFile image = new MockMultipartFile("file", "../test.png", MediaType.IMAGE_PNG_VALUE,
                pngBytes(16, 16));
        JsonNode uploaded = json(mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", productId)
                .file(image).param("altText", "Imagem sintética do perfume")
                .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.primary").value(true)).andReturn());
        assertThat(uploaded.get("url").asText()).contains("/content").doesNotContain("..", "target");

        mockMvc.perform(put("/api/v1/admin/products/{id}/status", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishable").value(true));

        mockMvc.perform(get("/api/v1/catalog/products/perfume-{suffix}".replace("{suffix}", suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentations.length()").value(2))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.description").value("Descrição sintética para teste."));
        mockMvc.perform(get("/api/v1/catalog/products").param("q", "PERFUME DE TESTE " + suffix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].slug").value("perfume-" + suffix));

        mockMvc.perform(put("/api/v1/admin/products/{id}/status", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false,\"version\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/products/perfume-" + suffix))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesBackendPermissionsAndImageContentValidation() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));

        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update("insert into perfumestore.categories (id, name, slug, active, version, created_at, updated_at) values (?, ?, ?, true, 0, ?, ?)",
                categoryId, "Categoria de validação", "categoria-validacao-" + productId.toString().substring(0, 8),
                Timestamp.from(now), Timestamp.from(now));
        jdbcTemplate.update("insert into perfumestore.products (id, slug, name, brand, description, category_id, active, version, created_at, updated_at) values (?, ?, ?, ?, ?, ?, false, 0, ?, ?)",
                productId, "produto-validacao-" + productId.toString().substring(0, 8), "Produto de validação", "Marca Teste",
                "Descrição de validação", categoryId, Timestamp.from(now), Timestamp.from(now));

        MockMultipartFile mismatched = new MockMultipartFile("file", "image.jpg", MediaType.IMAGE_JPEG_VALUE,
                pngBytes(4, 4));
        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", productId)
                .file(mismatched).param("altText", "Imagem inválida")
                .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("IMAGE_CONTENT_TYPE_MISMATCH"));
    }

    @Test
    void rejectsUnallowedPublicOrdering() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products").param("sort", "price,desc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void rejectsInvalidPresentationScaleAndStaleVersion() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode category = json(mockMvc.perform(post("/api/v1/admin/categories")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Categoria escala %s\",\"slug\":\"escala-%s\",\"active\":true}"
                        .formatted(suffix, suffix)))
                .andExpect(status().isCreated()).andReturn());
        JsonNode product = json(mockMvc.perform(post("/api/v1/admin/products")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Produto escala %s\",\"brand\":\"Marca Teste\",\"description\":\"Produto de validação.\",\"categoryId\":\"%s\",\"slug\":\"produto-escala-%s\",\"active\":false}"
                        .formatted(suffix, category.get("id").asText(), suffix)))
                .andExpect(status().isCreated()).andReturn());
        String productId = product.get("id").asText();

        mockMvc.perform(post("/api/v1/admin/products/{id}/presentations", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"SCALE-%s\",\"volume\":50,\"price\":199.999,\"active\":true}"
                        .formatted(suffix)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        JsonNode presentation = json(mockMvc.perform(post("/api/v1/admin/products/{id}/presentations", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"VALID-%s\",\"volume\":50,\"price\":199.90,\"active\":true}"
                        .formatted(suffix)))
                .andExpect(status().isCreated()).andReturn());
        String presentationId = presentation.get("id").asText();
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/presentations/{presentationId}", productId,
                presentationId).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"VALID-%s\",\"volume\":50,\"price\":209.90,\"active\":true,\"version\":0}"
                        .formatted(suffix)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/presentations/{presentationId}", productId,
                presentationId).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"VALID-%s\",\"volume\":50,\"price\":219.90,\"active\":true,\"version\":0}"
                        .formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void deactivatingCategoryHidesPublishedProductWithoutChangingProductState() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode category = json(mockMvc.perform(post("/api/v1/admin/categories")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Categoria estado %s\",\"slug\":\"estado-%s\",\"active\":true}"
                        .formatted(suffix, suffix)))
                .andExpect(status().isCreated()).andReturn());
        String categoryId = category.get("id").asText();
        JsonNode product = json(mockMvc.perform(post("/api/v1/admin/products")
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Produto estado %s\",\"brand\":\"Marca Teste\",\"description\":\"Produto publicado de teste.\",\"categoryId\":\"%s\",\"slug\":\"produto-estado-%s\",\"active\":false}"
                        .formatted(suffix, categoryId, suffix)))
                .andExpect(status().isCreated()).andReturn());
        String productId = product.get("id").asText();
        mockMvc.perform(post("/api/v1/admin/products/{id}/presentations", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"ESTADO-%s\",\"volume\":50,\"price\":199.90,\"active\":true}"
                        .formatted(suffix)))
                .andExpect(status().isCreated());
        MockMultipartFile image = new MockMultipartFile("file", "state.png", MediaType.IMAGE_PNG_VALUE,
                pngBytes(8, 8));
        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", productId).file(image)
                .param("altText", "Imagem de estado").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/v1/admin/products/{id}/status", productId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true,\"version\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/categories/{id}", categoryId)
                .with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Categoria estado %s\",\"slug\":\"estado-%s\",\"active\":false,\"version\":0}"
                        .formatted(suffix, suffix)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/v1/catalog/products/produto-estado-" + suffix))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("select active from perfumestore.products where id = ?",
                Boolean.class, UUID.fromString(productId))).isTrue();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
