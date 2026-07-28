package br.com.systemcommerce.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.service.SaleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
class DashboardReportModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_dashboard_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaleService saleService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private StoreProductService storeProductService;

    private String adminToken;
    private UUID adminUserId;
    private UUID customerId;
    private UUID loja01Id;
    private UUID dep01Id;
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
        var loginJson = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = loginJson.path("accessToken").asText();
        adminUserId = UUID.fromString(loginJson.path("user").path("id").asText());
        authenticateAs(adminUserId);

        var maria = customerRepository.findByDocument("52998224725").orElseThrow();
        maria.markActive();
        customerRepository.saveAndFlush(maria);
        customerId = maria.getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        loja01Id = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
        dep01Id = warehouseService.list(loja01Id, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> "DEP-01".equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    void shouldReturnDashboardIndicatorsFromBackend() throws Exception {
        Product product = createProduct(new BigDecimal("100.00"), new BigDecimal("50"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("5"), "seed", false));

        SaleResponse sale = prepareAndConfirm(product.getId(), BigDecimal.ONE);
        paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.PIX,
                new BigDecimal("100.00"),
                null,
                "DASH-TX",
                null,
                1,
                null,
                true));

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("topLimit", "5")
                        .param("periodDays", "14")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesToday.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.salesMonth.totalAmount").exists())
                .andExpect(jsonPath("$.data.averageTicketMonth").exists())
                .andExpect(jsonPath("$.data.topProducts").isArray())
                .andExpect(jsonPath("$.data.topCustomers").isArray())
                .andExpect(jsonPath("$.data.stockBelowMinimumCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.salesByStatus").isArray())
                .andExpect(jsonPath("$.data.salesByPeriod").isArray())
                .andExpect(jsonPath("$.data.receiptsByPaymentMethod").isArray());
    }

    @Test
    void shouldFilterSalesReportAndExportCsv() throws Exception {
        Product product = createProduct(new BigDecimal("40.00"), BigDecimal.ZERO);
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("20"), "seed", false));
        prepareAndConfirm(product.getId(), new BigDecimal("2"));

        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        MvcResult csv = mockMvc.perform(get("/api/v1/reports/sales/csv")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = csv.getResponse().getContentAsString();
        assertThat(body).contains("Numero").contains("Total");
        assertThat(csv.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("vendas.csv");
    }

    @Test
    void shouldAggregatePaymentsByMethodAndRequirePermission() throws Exception {
        Product product = createProduct(new BigDecimal("25.00"), BigDecimal.ZERO);
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse sale = prepareAndConfirm(product.getId(), BigDecimal.ONE);
        paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.CASH,
                new BigDecimal("25.00"),
                null,
                null,
                null,
                1,
                new BigDecimal("25.00"),
                true));

        mockMvc.perform(get("/api/v1/reports/payments/by-method")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amount").exists());

        mockMvc.perform(get("/api/v1/reports/inventory/below-minimum")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }

    private SaleResponse prepareAndConfirm(UUID productId, BigDecimal quantity) {
        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, null));
        if (draft.customerId() == null) {
            saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        }
        saleService.addItem(
                draft.id(), new SaleItemRequest(productId, quantity, null, BigDecimal.ZERO, null));
        return saleService.confirm(draft.id());
    }

    private Product createProduct(BigDecimal salePrice, BigDecimal minStock) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("DASH-" + suffix);
        product.setSku("SKU-DASH-" + suffix);
        product.setName("Produto dash " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(salePrice);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(minStock);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        return saved;
    }

    private void authenticateAs(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                java.util.List.of(
                        new SimpleGrantedAuthority("SALE_CREATE"),
                        new SimpleGrantedAuthority("SALE_CONFIRM"),
                        new SimpleGrantedAuthority("SALE_READ"),
                        new SimpleGrantedAuthority("PAYMENT_MANAGE"),
                        new SimpleGrantedAuthority("INVENTORY_MOVE"),
                        new SimpleGrantedAuthority("DASHBOARD_READ"),
                        new SimpleGrantedAuthority("REPORT_READ"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
