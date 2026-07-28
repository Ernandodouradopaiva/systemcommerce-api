package br.com.systemcommerce.pos.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.pos.report.dto.PosReportType;
import br.com.systemcommerce.pos.report.service.PosDashboardService;
import br.com.systemcommerce.pos.report.service.PosReportService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
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
import org.springframework.data.domain.PageRequest;
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
class PosReportModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_report_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosReportService posReportService;

    @Autowired
    private PosDashboardService posDashboardService;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private PosTerminalService posTerminalService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryService inventoryService;

    private String adminToken;
    private UUID adminUserId;
    private UUID terminalId;
    private UUID cashSessionId;
    private UUID warehouseId;
    private UUID storeId;
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
        var data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = data.path("accessToken").asText();
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withSecurity(() -> {
            var available = posTerminalService.listAvailable(null, Pageable.unpaged());
            var terminal = available.getContent().stream()
                    .filter(t -> "TERM-01".equals(t.code()))
                    .findFirst()
                    .orElseGet(() -> available.getContent().getFirst());
            terminalId = terminal.id();
            warehouseId = terminal.warehouseId();
            storeId = terminal.storeId();

            cashSessionRepository.findActiveByTerminalId(terminalId).ifPresent(active -> {
                var recon = cashSessionService.reconcile(active.getId());
                cashSessionService.close(
                        active.getId(),
                        new CashSessionCloseRequest(recon.expectedCash(), "cleanup"),
                        "cleanup-rpt-" + UUID.randomUUID());
            });

            var session = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), null),
                    "rpt-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldAggregateSalesByStoreAndExportCsv() throws Exception {
        withSecurity(() -> {
            var sale = createPaidSale(new BigDecimal("50.00"));
            assertThat(sale.id()).isNotNull();

            var page = posReportService.aggregate(
                    PosReportType.SALES_BY_STORE,
                    new PosReportFilter(null, null, storeId, null, null, null, null, null, null, null),
                    PageRequest.of(0, 50));
            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().stream().anyMatch(r -> r.totalAmount().compareTo(BigDecimal.ZERO) > 0))
                    .isTrue();
        });

        mockMvc.perform(get("/api/v1/pos/reports/SALES_BY_STORE/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", storeId.toString())
                        .param("format", "CSV"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExportPdf() throws Exception {
        withSecurity(() -> createPaidSale(new BigDecimal("12.00")));

        MvcResult result = mockMvc.perform(get("/api/v1/pos/reports/SALES_BY_STORE/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", storeId.toString())
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThan(20);
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void shouldReturnDashboardKpis() {
        withSecurity(() -> {
            createPaidSale(new BigDecimal("35.00"));
            var dash = posDashboardService.summary(storeId, terminalId);
            assertThat(dash.salesToday().count()).isGreaterThanOrEqualTo(1);
            assertThat(dash.openCashSessions()).isGreaterThanOrEqualTo(1);
            assertThat(dash.salesByHour()).isNotNull();
            assertThat(dash.paymentsByMethod()).isNotNull();
        });
    }

    @Test
    void shouldListSalesByHourViaApi() throws Exception {
        withSecurity(() -> createPaidSale(new BigDecimal("20.00")));

        mockMvc.perform(get("/api/v1/pos/reports/period/hourly")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private br.com.systemcommerce.sale.dto.SaleResponse createPaidSale(BigDecimal price) {
        Product product = createProduct(price);
        seedStock(product.getId(), "50");
        var sale = posSaleService.start(new PosSaleStartRequest(cashSessionId), "rpt-start-" + UUID.randomUUID());
        sale = posSaleService.addByProductId(
                sale.id(),
                new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, sale.version()),
                null);
        posCheckoutService.addPayment(
                sale.id(),
                new PosPaymentAddRequest(
                        Payment.PaymentMethod.CASH,
                        price,
                        price,
                        1,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false),
                "pay-" + UUID.randomUUID());
        posCheckoutService.finalizeSale(sale.id(), "fin-" + UUID.randomUUID());
        return posSaleService.summary(sale.id());
    }

    private Product createProduct(BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("RPT-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto relatório " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(price);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        return saved;
    }

    private void seedStock(UUID productId, String qty) {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed rpt", false));
    }

    private void withSecurity(Runnable action) {
        withSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withSecurity(java.util.concurrent.Callable<T> action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            adminUserId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                    new SimpleGrantedAuthority("POS_CLOSE_CASH"),
                                    new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                    new SimpleGrantedAuthority("POS_FORCE_CLOSE_CASH"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                    new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                    new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                    new SimpleGrantedAuthority("SALE_CONFIRM"),
                                    new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                    new SimpleGrantedAuthority("INVENTORY_READ"),
                                    new SimpleGrantedAuthority("STORE_MANAGE"),
                                    new SimpleGrantedAuthority("POS_REPORT_READ"),
                                    new SimpleGrantedAuthority("POS_REPORT_EXPORT"),
                                    new SimpleGrantedAuthority("POS_DASHBOARD_READ"),
                                    new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
