package br.com.systemcommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.service.CategoryService;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProductCategoryModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_product_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private String adminToken;
    private UUID informaticaId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper
                .readTree(login.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        informaticaId = categoryRepository
                .findByNameIgnoreCase("Informática")
                .orElseThrow()
                .getId();
    }

    @Test
    void shouldCreateCategoryAndProductAndEnforceUniqueness() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Games","description":"Jogos","parentId":null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Games"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Games","description":"dup"}
                                """))
                .andExpect(status().isConflict());

        String productJson =
                """
                {
                  "internalCode":"INT-KB-001",
                  "sku":"KB-001",
                  "barcode":"7891999000101",
                  "name":"Teclado mecânico",
                  "description":"Switch blue",
                  "categoryId":"%s",
                  "unitOfMeasure":"UN",
                  "costPrice":80.00,
                  "salePrice":149.90,
                  "minStock":3,
                  "allowNegativeStock":false,
                  "imageUrl":"https://cdn.example.com/kb.jpg"
                }
                """
                        .formatted(informaticaId);

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("KB-001"))
                .andExpect(jsonPath("$.data.salePrice").value(149.90));

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldRejectNegativePricesAndInactiveCategoryForNewProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "internalCode":"BAD-1",
                                  "sku":"BAD-1",
                                  "name":"Ruim",
                                  "categoryId":"%s",
                                  "unitOfMeasure":"UN",
                                  "costPrice":-1,
                                  "salePrice":10,
                                  "minStock":0,
                                  "allowNegativeStock":false
                                }
                                """
                                        .formatted(informaticaId)))
                .andExpect(status().isBadRequest());

        Category category = categoryRepository.findByNameIgnoreCase("Serviços").orElseThrow();
        mockMvc.perform(patch("/api/v1/categories/" + category.getId() + "/inactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "internalCode":"NEW-IN-INACTIVE",
                                  "sku":"NEW-IN-INACTIVE",
                                  "name":"Não deve",
                                  "categoryId":"%s",
                                  "unitOfMeasure":"UN",
                                  "costPrice":1,
                                  "salePrice":2,
                                  "minStock":0,
                                  "allowNegativeStock":false
                                }
                                """
                                        .formatted(category.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Categoria inativa não pode receber novo produto"));

        // restore for other tests sharing container across methods - activate again
        mockMvc.perform(patch("/api/v1/categories/" + category.getId() + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFilterProductsAndBlockInactiveForSale() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("sku", "NB-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sku").value("NB-001"));

        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", informaticaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").isNumber());

        Product notebook = productRepository.findBySkuIgnoreCase("NB-001").orElseThrow();
        mockMvc.perform(patch("/api/v1/products/" + notebook.getId() + "/inactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        assertThatThrownBy(() -> productService.requireUsableForSale(notebook.getId()))
                .isInstanceOf(BusinessRuleException.class);

        mockMvc.perform(patch("/api/v1/products/" + notebook.getId() + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(productService.requireUsableForSale(notebook.getId()).isUsableForSale()).isTrue();
    }

    @Test
    void shouldListCategoriesByStatusAndParent() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .param("status", "ACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/categories")
                        .param("parentId", informaticaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Periféricos"));
    }
}
