package br.com.systemcommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.dashboard.service.DashboardService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.pos.cancellation.dto.CancellationDecisionRequest;
import br.com.systemcommerce.pos.cancellation.dto.CancellationRequestCreate;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.service.PosCancellationService;
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
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByBarcodeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleCustomerRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;
import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.service.PriceTableService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/**
 * Integração ERP administrativo ↔ PDV (Prompt 54): mesma fonte de verdade (products, prices,
 * customers, sales, payments, inventory, users, audit_logs).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ErpPosIntegrationModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_erp_pos_integration_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosTerminalService posTerminalService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private PosCancellationService posCancellationService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PriceTableService priceTableService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private PosDashboardService posDashboardService;

    @Autowired
    private PosReportService posReportService;

    private String adminToken;
    private UUID adminUserId;
    private UUID terminalId;
    private UUID warehouseId;
    private UUID cashSessionId;
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
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();

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
                        new CashSessionCloseRequest(recon.expectedCash(), "cleanup integration"),
                        "cleanup-int-" + UUID.randomUUID());
            });
            discardDraftIfAny();
            var session = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("50.00"), "integration"),
                    "int-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });
    }

    @Test
    @DisplayName("ERP→PDV→ERP: produto/preço/cliente/estoque/venda/pagamento/dashboard/cancelamento")
    void shouldIntegrateErpCatalogCashSalePaymentsInventoryAndReports() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String barcode = "789" + suffix.substring(0, 7);

        UUID saleId = withSecurity(() -> {
            // 1) Produto + preço (tabela ERP) + estoque no depósito do terminal
            Product product = createProduct(barcode, new BigDecimal("100.00"));
            seedStock(product.getId(), "20");
            BigDecimal stockBefore = inventoryService.availableQuantity(product.getId(), warehouseId);

            var table = priceTableService.create(new PriceTableCreateRequest(
                    "INT-" + suffix,
                    "Integração " + suffix,
                    null,
                    10,
                    PriceChannel.POS,
                    PriceTableScopeType.STORE,
                    null,
                    null,
                    null));
            priceTableService.linkStore(table.id(), storeId);
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("80.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            10,
                            null,
                            null,
                            null));

            var customer = customerService.create(new CustomerCreateRequest(
                    Customer.CustomerType.PF,
                    "Cliente Integração " + suffix,
                    null,
                    randomValidCpf(),
                    null,
                    "int+" + suffix + "@example.com",
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
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));

            // 2) Venda PDV usa produto/preço/cliente ERP
            discardDraftIfAny();
            var sale = posSaleService.start(new PosSaleStartRequest(cashSessionId), "int-start-" + suffix);
            assertThat(sale.channel()).isEqualTo(Sale.SaleChannel.POS);
            assertThat(sale.warehouseId()).isEqualTo(warehouseId);

            sale = posSaleService.addByBarcode(
                    sale.id(),
                    new PosSaleAddByBarcodeRequest(barcode, BigDecimal.ONE, sale.version()),
                    "int-bar-" + suffix);
            assertThat(sale.items().getFirst().unitPrice()).isEqualByComparingTo("80.00");
            assertThat(sale.totalAmount()).isEqualByComparingTo("80.00");

            sale = posSaleService.identifyCustomer(
                    sale.id(), new PosSaleCustomerRequest(customer.id(), sale.version()), "int-cust-" + suffix);
            assertThat(sale.customerId()).isEqualTo(customer.id());

            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("80.00"),
                            null,
                            1,
                            "PIX-INT-" + suffix,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    "int-pix-" + suffix);
            var finalized = posCheckoutService.finalizeSale(sale.id(), "int-fin-" + suffix);
            assertThat(finalized.status()).isEqualTo(Sale.SaleStatus.PAID);
            var paidSale = finalized.sale();
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo(stockBefore.subtract(BigDecimal.ONE));

            // 3) Mesma venda na listagem ERP (tabela sales; canal POS)
            var erpList = saleService.list(
                    null,
                    null,
                    null,
                    null,
                    paidSale.saleNumber(),
                    null,
                    null,
                    null,
                    null,
                    PageRequest.of(0, 20));
            assertThat(erpList.getContent())
                    .anyMatch(s -> s.id().equals(paidSale.id()) && s.channel() == Sale.SaleChannel.POS);

            var onlyPos = saleService.list(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Sale.SaleChannel.POS,
                    PageRequest.of(0, 50));
            assertThat(onlyPos.getContent()).anyMatch(s -> s.id().equals(paidSale.id()));

            // 4) Pagamentos na mesma tabela payments
            var payments = paymentService.listBySale(paidSale.id());
            assertThat(payments).isNotEmpty();
            assertThat(payments.getFirst().method()).isEqualTo(Payment.PaymentMethod.PIX);
            assertThat(payments.getFirst().amount()).isEqualByComparingTo("80.00");

            // 5) Movimentação oficial de estoque SALE no depósito
            var movements = inventoryService.listMovements(
                    product.getId(),
                    warehouseId,
                    InventoryMovement.MovementType.SALE,
                    null,
                    null,
                    PageRequest.of(0, 20));
            assertThat(movements.getContent()).isNotEmpty();

            // 6) Dashboards / relatório PDV enxergam a sessão/venda
            var adminDash = dashboardService.summary(5, 14, null, null);
            assertThat(adminDash.salesToday().count()).isGreaterThanOrEqualTo(1);
            var posDash = posDashboardService.summary(storeId, terminalId);
            assertThat(posDash.salesToday().count()).isGreaterThanOrEqualTo(1);
            var report = posReportService.aggregate(
                    PosReportType.SALES_BY_STORE,
                    new PosReportFilter(null, null, storeId, null, null, null, null, null, null, null),
                    PageRequest.of(0, 20));
            assertThat(report.getContent()).isNotEmpty();

            // 7) Cancelamento restaura estoque (SALE_CANCEL)
            var cancelReq = posCancellationService.request(
                    new CancellationRequestCreate(paidSale.id(), "teste integração"), "int-cancel-" + suffix);
            posCancellationService.authorize(cancelReq.id(), new CancellationDecisionRequest("ok"));
            var cancelled = posCancellationService.execute(cancelReq.id());
            assertThat(cancelled.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo(stockBefore);

            var cancelMoves = inventoryService.listMovements(
                    product.getId(),
                    warehouseId,
                    InventoryMovement.MovementType.SALE_CANCEL,
                    null,
                    null,
                    PageRequest.of(0, 20));
            assertThat(cancelMoves.getContent()).isNotEmpty();

            return paidSale.id();
        });

        // 8) HTTP ERP: venda POS listável; auditoria central
        mockMvc.perform(get("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("channel", "POS")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='%s')].channel", saleId).exists());

        mockMvc.perform(get("/api/v1/sales/" + saleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel").value("POS"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("size", "5"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pos/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    private Product createProduct(String barcode, BigDecimal catalogPrice) {
        Product product = new Product();
        product.setInternalCode("INT-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setBarcode(barcode);
        product.setName("Produto Integração " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(catalogPrice);
        product.setCostPrice(BigDecimal.TEN);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        // Habilita o produto na loja do terminal para que seja vendável via PDV
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        return saved;
    }

    private void seedStock(UUID productId, String qty) {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed integration", false));
    }

    private void discardDraftIfAny() {
        try {
            var current = posSaleService.currentByTerminal(terminalId);
            if (current.status() == Sale.SaleStatus.DRAFT || current.status() == Sale.SaleStatus.SUSPENDED) {
                posSaleService.discard(
                        current.id(),
                        new PosSaleDiscardRequest("limpeza integration", current.version()),
                        "discard-int-" + UUID.randomUUID());
            }
        } catch (Exception ignored) {
            // sem rascunho
        }
    }

    private static String randomValidCpf() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(0, 10);
        }
        d[9] = cpfDigit(d, 9);
        d[10] = cpfDigit(d, 10);
        StringBuilder sb = new StringBuilder(11);
        for (int value : d) {
            sb.append(value);
        }
        return sb.toString();
    }

    private static int cpfDigit(int[] digits, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * (weight - i);
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private void withSecurity(Runnable action) {
        withSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withSecurity(java.util.concurrent.Callable<T> action) {
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
                                new SimpleGrantedAuthority("POS_SALE_ITEM_REMOVE"),
                                new SimpleGrantedAuthority("POS_SALE_DISCOUNT"),
                                new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                new SimpleGrantedAuthority("POS_CANCEL_DRAFT"),
                                new SimpleGrantedAuthority("POS_CANCEL_COMPLETED_SALE"),
                                new SimpleGrantedAuthority("POS_CANCEL_AUTHORIZE"),
                                new SimpleGrantedAuthority("POS_REFUND_EXECUTE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_REFUND"),
                                new SimpleGrantedAuthority("POS_REPORT_READ"),
                                new SimpleGrantedAuthority("POS_DASHBOARD_READ"),
                                new SimpleGrantedAuthority("POS_AUDIT_READ"),
                                new SimpleGrantedAuthority("SALE_READ"),
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("SALE_CANCEL"),
                                new SimpleGrantedAuthority("PAYMENT_READ"),
                                new SimpleGrantedAuthority("PAYMENT_CREATE"),
                                new SimpleGrantedAuthority("PAYMENT_CONFIRM"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("INVENTORY_ADJUST"),
                                new SimpleGrantedAuthority("INVENTORY_ENTRY"),
                                new SimpleGrantedAuthority("PRODUCT_READ"),
                                new SimpleGrantedAuthority("PRODUCT_CREATE"),
                                new SimpleGrantedAuthority("CUSTOMER_READ"),
                                new SimpleGrantedAuthority("CUSTOMER_CREATE"),
                                new SimpleGrantedAuthority("PRICE_TABLE_MANAGE"),
                                new SimpleGrantedAuthority("PRICE_TABLE_READ"),
                                new SimpleGrantedAuthority("DASHBOARD_READ"),
                                new SimpleGrantedAuthority("AUDIT_READ"),
                                new SimpleGrantedAuthority("STORE_READ"),
                                new SimpleGrantedAuthority("WAREHOUSE_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
