package br.com.systemcommerce.storeproduct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.storeproduct.dto.StoreProductBlockRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductBulkEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductUpdateRequest;
import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.repository.StoreProductRepository;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
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
class StoreProductModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_store_product_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private StoreProductRepository storeProductRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String adminToken;
    private UUID loja01Id;
    private UUID loja02Id;
    private Category category;

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

        loja01Id = storeRepository.findByCodeIgnoreCase("LOJA-01").orElseThrow().getId();
        loja02Id = storeRepository.findByCodeIgnoreCase("LOJA-02").orElseThrow().getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldEnableProductOnStore() throws Exception {
        Product product = createProduct("SP-EN-001");

        mockMvc.perform(post("/api/v1/store-products/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StoreProductEnableRequest(loja01Id, product.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.storeId").value(loja01Id.toString()))
                .andExpect(jsonPath("$.data.productId").value(product.getId().toString()));

        mockMvc.perform(get("/api/v1/store-products/availability")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("productId", product.getId().toString())
                        .param("storeId", loja01Id.toString())
                        .param("channel", "POS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sellable").value(true));
    }

    @Test
    void shouldBlockProductAndRejectPdvSellability() throws Exception {
        Product product = createProduct("SP-BL-001");
        var enabled = storeProductService.enable(new StoreProductEnableRequest(loja01Id, product.getId()));

        storeProductService.block(enabled.id(), new StoreProductBlockRequest("Recall do lote"));

        var availability = storeProductService.checkAvailability(product.getId(), loja01Id, SaleChannel.POS);
        assertThat(availability.sellable()).isFalse();
        assertThat(availability.reason()).contains("ativo nesta loja");

        assertThatThrownBy(() -> storeProductService.assertSellable(product.getId(), loja01Id, SaleChannel.POS))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectGloballyInactiveProduct() throws Exception {
        Product product = createProduct("SP-IN-001");
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, product.getId()));

        product.markInactive();
        productRepository.saveAndFlush(product);

        var availability = storeProductService.checkAvailability(product.getId(), loja01Id, SaleChannel.ERP);
        assertThat(availability.sellable()).isFalse();
        assertThat(availability.reason()).contains("inativo globalmente");

        assertThatThrownBy(() -> storeProductService.enable(new StoreProductEnableRequest(loja02Id, product.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo globalmente");
    }

    @Test
    void shouldBulkEnableProductOnMultipleStores() throws Exception {
        Product product = createProduct("SP-BK-001");

        mockMvc.perform(post("/api/v1/store-products/bulk-enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StoreProductBulkEnableRequest(product.getId(), List.of(loja01Id, loja02Id)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));

        assertThat(storeProductRepository.findByStoreIdAndProductId(loja01Id, product.getId()))
                .isPresent();
        assertThat(storeProductRepository.findByStoreIdAndProductId(loja02Id, product.getId()))
                .isPresent();
    }

    @Test
    void shouldEnforceUniqueLocalBarcodePerStore() {
        Product first = createProduct("SP-BC-001");
        Product second = createProduct("SP-BC-002");
        var firstConfig = storeProductService.enable(new StoreProductEnableRequest(loja01Id, first.getId()));
        var secondConfig = storeProductService.enable(new StoreProductEnableRequest(loja01Id, second.getId()));

        storeProductService.update(
                firstConfig.id(),
                new StoreProductUpdateRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "LOCAL-7899000001",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThatThrownBy(() -> storeProductService.update(
                        secondConfig.id(),
                        new StoreProductUpdateRequest(
                                null,
                                null,
                                null,
                                null,
                                null,
                                "LOCAL-7899000001",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)))
                .matches(
                        ex -> ex instanceof ConflictException
                                || ex instanceof org.springframework.dao.DataIntegrityViolationException
                                || (ex.getMessage() != null && ex.getMessage().contains("uk_store_products_store_barcode")),
                        "deve rejeitar barcode local duplicado na loja");
    }

    private Product createProduct(String sku) {
        Product product = new Product();
        product.setSku(sku);
        product.setInternalCode("INT-" + sku);
        product.setName("Produto store " + sku);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(new BigDecimal("99.90"));
        product.setCostPrice(new BigDecimal("50.00"));
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        return productRepository.saveAndFlush(product);
    }
}
