package br.com.systemcommerce.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cancellation.dto.CancellationDecisionRequest;
import br.com.systemcommerce.pos.cancellation.dto.CancellationRequestCreate;
import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.service.PosCancellationService;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.dto.CashSupplyRequest;
import br.com.systemcommerce.pos.cash.dto.CashWithdrawalRequest;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashMovementService;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintRequest;
import br.com.systemcommerce.pos.receipt.dto.ReceiptReprintRequest;
import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import br.com.systemcommerce.pos.receipt.service.PosReceiptService;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.pos.report.dto.PosReportType;
import br.com.systemcommerce.pos.report.service.PosReportService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByBarcodeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleCustomerRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleHeaderDiscountRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleQuantityRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleResumeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSuspendRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.exception.ConflictException;
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
 * Fluxo ponta a ponta do PDV (Prompt 50): loja → caixa → venda → estoque → cancelamento →
 * fechamento → relatório/auditoria. Regras comerciais validadas apenas na API (PostgreSQL real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PosFullFlowModuleTest {

    private static final UUID REASON_SUPPLY = UUID.fromString("c2000000-0000-4000-8000-000000000001");
    private static final UUID REASON_WITHDRAW = UUID.fromString("c2000000-0000-4000-8000-000000000004");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_full_flow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private PosTerminalService posTerminalService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private CashMovementService cashMovementService;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private PosCancellationService posCancellationService;

    @Autowired
    private PosReceiptService posReceiptService;

    @Autowired
    private PosReportService posReportService;

    @Autowired
    private CustomerService customerService;

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
    }

    @Test
    @DisplayName("fluxo completo PDV: cadastro → caixa → venda → estoque → sangria → cancelamento → fechamento")
    void shouldExecuteCompletePosOperatorFlow() {
        withFullSecurity(() -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // 1) Loja, depósito e terminal
            var store = storeService.create(new StoreCreateRequest(
                    null,
                    "LJ-" + suffix,
                    "Loja Full Flow " + suffix,
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
                    "Curitiba",
                    "PR",
                    "America/Sao_Paulo"));
            var warehouse = warehouseService.create(
                    new WarehouseCreateRequest(store.id(), "DEP-" + suffix, "Depósito " + suffix, true));
            var terminal = posTerminalService.create(new PosTerminalCreateRequest(
                    store.id(),
                    warehouse.id(),
                    "T-" + suffix,
                    "Terminal Full " + suffix,
                    ThreadLocalRandom.current().nextInt(100, 999),
                    null,
                    null,
                    null));
            assertThat(terminal.eligibleToOpenCashSession()).isTrue();

            // 2) Abertura + rejeição concorrente
            var session = cashSessionService.open(
                    new CashSessionOpenRequest(terminal.id(), new BigDecimal("200.00"), "abertura full"),
                    "full-open-" + suffix);
            assertThat(session.status()).isEqualTo(CashSession.CashSessionStatus.OPEN);
            assertThatThrownBy(() -> cashSessionService.open(
                            new CashSessionOpenRequest(terminal.id(), new BigDecimal("10.00"), null),
                            "full-open-dup-" + suffix))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("sessão aberta");

            // 3) Suprimento
            cashMovementService.registerSupply(
                    session.id(),
                    new CashSupplyRequest(
                            session.id(), new BigDecimal("50.00"), REASON_SUPPLY, "fundo de troco", null),
                    "full-supply-" + suffix);

            // 4) Produto + estoque + cliente
            Product product = createProduct(store.id(), "789" + suffix + "0", new BigDecimal("100.00"));
            seedStock(product.getId(), warehouse.id(), "20");
            BigDecimal stockBefore = inventoryService.availableQuantity(product.getId(), warehouse.id());
            var customer = customerService.create(new CustomerCreateRequest(
                    Customer.CustomerType.PF,
                    "Cliente Full Flow",
                    null,
                    randomValidCpf(),
                    null,
                    "full+" + suffix + "@example.com",
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

            // 5) Venda: barcode, qty, cliente, desconto, suspend/resume
            discardDraftIfAny(terminal.id());
            var sale = posSaleService.start(new PosSaleStartRequest(session.id()), "full-start-" + suffix);
            sale = posSaleService.addByBarcode(
                    sale.id(),
                    new PosSaleAddByBarcodeRequest(product.getBarcode(), BigDecimal.ONE, sale.version()),
                    "full-bar-" + suffix);
            assertThat(sale.items()).hasSize(1);
            assertThat(sale.totalAmount()).isEqualByComparingTo("100.00");

            sale = posSaleService.updateQuantity(
                    sale.id(),
                    sale.items().getFirst().id(),
                    new PosSaleQuantityRequest(new BigDecimal("2"), sale.version()),
                    "full-qty-" + suffix);
            assertThat(sale.totalAmount()).isEqualByComparingTo("200.00");

            sale = posSaleService.identifyCustomer(
                    sale.id(), new PosSaleCustomerRequest(customer.id(), sale.version()), "full-cust-" + suffix);
            assertThat(sale.customerId()).isEqualTo(customer.id());

            sale = posSaleService.headerDiscount(
                    sale.id(),
                    new PosSaleHeaderDiscountRequest(new BigDecimal("20.00"), sale.version(), null),
                    "full-disc-" + suffix);
            assertThat(sale.discountAmount()).isEqualByComparingTo("20.00");
            assertThat(sale.totalAmount()).isEqualByComparingTo("180.00");

            posSaleService.suspend(
                    sale.id(),
                    new PosSaleSuspendRequest("cliente foi ao banheiro", sale.version()),
                    "full-susp-" + suffix);
            sale = posSaleService.resume(
                    sale.id(), new PosSaleResumeRequest(session.id(), null), "full-resume-" + suffix);
            assertThat(sale.status()).isEqualTo(Sale.SaleStatus.DRAFT);
            assertThat(sale.totalAmount()).isEqualByComparingTo("180.00");

            // 6) Pagamento parcial + múltiplos (PIX + dinheiro com troco) + finalização
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("80.00"),
                            null,
                            1,
                            "PIX-" + suffix,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    "full-pix-" + suffix);
            var cashPay = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("100.00"),
                            new BigDecimal("120.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    "full-cash-" + suffix);
            assertThat(cashPay.changeAmount()).isEqualByComparingTo("20.00");

            var finalized = posCheckoutService.finalizeSale(sale.id(), "full-fin-" + suffix);
            assertThat(finalized.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(finalized.balanceDue()).isEqualByComparingTo("0.00");
            assertThat(finalized.changeTotal()).isEqualByComparingTo("20.00");
            assertThat(finalized.printData()).isNotNull();
            assertThat(inventoryService.availableQuantity(product.getId(), warehouse.id()))
                    .isEqualByComparingTo(stockBefore.subtract(new BigDecimal("2")));

            // 7) Impressão / reimpressão
            var printed = posReceiptService.registerPrint(new ReceiptPrintRequest(
                    ReceiptPrintLog.PrintType.SALE,
                    sale.id(),
                    null,
                    null,
                    null,
                    null,
                    ReceiptPrintLog.PrintLayout.THERMAL_80,
                    1,
                    null));
            assertThat(printed.reprint()).isFalse();
            var reprinted = posReceiptService.registerReprint(new ReceiptReprintRequest(
                    printed.printLogId(),
                    ReceiptPrintLog.PrintType.SALE,
                    sale.id(),
                    null,
                    null,
                    null,
                    null,
                    ReceiptPrintLog.PrintLayout.THERMAL_80,
                    1,
                    "cliente solicitou 2ª via",
                    null));
            assertThat(reprinted.reprint()).isTrue();

            // 8) Sangria
            cashMovementService.registerWithdrawal(
                    session.id(),
                    new CashWithdrawalRequest(
                            session.id(), new BigDecimal("30.00"), REASON_WITHDRAW, "cofre", null, null),
                    "full-sangria-" + suffix);

            // 9) Segunda venda → cancelamento + estorno
            Product product2 = createProduct(store.id(), "788" + suffix + "1", new BigDecimal("40.00"));
            seedStock(product2.getId(), warehouse.id(), "5");
            BigDecimal stock2Before = inventoryService.availableQuantity(product2.getId(), warehouse.id());
            discardDraftIfAny(terminal.id());
            var sale2 = posSaleService.start(new PosSaleStartRequest(session.id()), "full-s2-" + suffix);
            sale2 = posSaleService.addByProductId(
                    sale2.id(),
                    new PosSaleAddByProductIdRequest(product2.getId(), BigDecimal.ONE, sale2.version()),
                    "full-s2-item-" + suffix);
            posCheckoutService.addPayment(
                    sale2.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("40.00"),
                            new BigDecimal("40.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    "full-s2-pay-" + suffix);
            posCheckoutService.finalizeSale(sale2.id(), "full-s2-fin-" + suffix);
            assertThat(inventoryService.availableQuantity(product2.getId(), warehouse.id()))
                    .isEqualByComparingTo(stock2Before.subtract(BigDecimal.ONE));

            var cancelReq = posCancellationService.request(
                    new CancellationRequestCreate(sale2.id(), "erro de operador"), "full-cancel-" + suffix);
            posCancellationService.authorize(cancelReq.id(), new CancellationDecisionRequest("ok gerente"));
            var cancelled = posCancellationService.execute(cancelReq.id());
            assertThat(cancelled.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(cancelled.refunds().getFirst().status()).isEqualTo(CancellationRefund.Status.COMPLETED);
            assertThat(inventoryService.availableQuantity(product2.getId(), warehouse.id()))
                    .isEqualByComparingTo(stock2Before);

            // 10) Fechamento com diferença
            var recon = cashSessionService.reconcile(session.id());
            BigDecimal counted = recon.expectedCash().subtract(new BigDecimal("5.00"));
            var closed = cashSessionService.close(
                    session.id(),
                    new CashSessionCloseRequest(counted, "diferença de R$ 5,00 justificada"),
                    "full-close-" + suffix);
            assertThat(closed.status()).isEqualTo(CashSession.CashSessionStatus.CLOSED);
            assertThat(closed.differenceAmount()).isEqualByComparingTo("-5.00");

            // 11) Relatório
            var report = posReportService.aggregate(
                    PosReportType.SALES_BY_STORE,
                    new PosReportFilter(null, null, store.id(), null, null, null, null, null, null, null),
                    PageRequest.of(0, 20));
            assertThat(report.getContent()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("auditoria PDV e permissão: consulta autenticada após abertura")
    void shouldQueryPosAuditAfterCashOpen() throws Exception {
        var available = withFullSecurity(() -> {
            var list = posTerminalService.listAvailable(null, Pageable.unpaged());
            return list.getContent().stream()
                    .filter(t -> "TERM-01".equals(t.code()))
                    .findFirst()
                    .orElseGet(() -> list.getContent().getFirst());
        });
        UUID terminalId = available.id();
        UUID storeId = available.storeId();

        withFullSecurity(() -> {
            cashSessionRepository.findActiveByTerminalId(terminalId).ifPresent(active -> {
                var recon = cashSessionService.reconcile(active.getId());
                cashSessionService.close(
                        active.getId(),
                        new CashSessionCloseRequest(recon.expectedCash(), "cleanup audit"),
                        "cleanup-audit-" + UUID.randomUUID());
            });
            cashSessionRepository.findOpenByOperatorId(adminUserId).forEach(active -> {
                var recon = cashSessionService.reconcile(active.getId());
                cashSessionService.close(
                        active.getId(),
                        new CashSessionCloseRequest(recon.expectedCash(), "cleanup audit operator"),
                        "cleanup-audit-op-" + UUID.randomUUID());
            });
        });

        UUID sessionId = withFullSecurity(() -> cashSessionService
                .open(
                        new CashSessionOpenRequest(terminalId, new BigDecimal("80.00"), "audit flow"),
                        "audit-flow-open-" + UUID.randomUUID())
                .id());

        mockMvc.perform(get("/api/v1/pos/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("eventCode", "CASH_OPEN")
                        .param("storeId", storeId.toString())
                        .param("terminalId", terminalId.toString())
                        .param("cashSessionId", sessionId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].eventCode").value("CASH_OPEN"));
    }

    private Product createProduct(UUID storeId, String barcode, BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("FF-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-FF-" + UUID.randomUUID().toString().substring(0, 8));
        product.setBarcode(barcode);
        product.setName("Produto Full Flow " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(price);
        product.setCostPrice(BigDecimal.TEN);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        return saved;
    }

    private void seedStock(UUID productId, UUID warehouseId, String qty) {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed full flow", false));
    }

    private void discardDraftIfAny(UUID terminalId) {
        try {
            var current = posSaleService.currentByTerminal(terminalId);
            if (current.status() == Sale.SaleStatus.DRAFT || current.status() == Sale.SaleStatus.SUSPENDED) {
                posSaleService.discard(
                        current.id(),
                        new PosSaleDiscardRequest("limpeza full flow", current.version()),
                        "discard-ff-" + UUID.randomUUID());
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

    private void withFullSecurity(Runnable action) {
        withFullSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withFullSecurity(java.util.concurrent.Callable<T> action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_CLOSE_CASH"),
                                new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                new SimpleGrantedAuthority("POS_FORCE_CLOSE_CASH"),
                                new SimpleGrantedAuthority("STORE_MANAGE"),
                                new SimpleGrantedAuthority("WAREHOUSE_MANAGE"),
                                new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                new SimpleGrantedAuthority("POS_SALE_ITEM_REMOVE"),
                                new SimpleGrantedAuthority("POS_SALE_DISCOUNT"),
                                new SimpleGrantedAuthority("POS_SALE_HIGH_DISCOUNT"),
                                new SimpleGrantedAuthority("POS_SALE_SUSPEND"),
                                new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                new SimpleGrantedAuthority("POS_CANCEL_DRAFT"),
                                new SimpleGrantedAuthority("POS_CANCEL_COMPLETED_SALE"),
                                new SimpleGrantedAuthority("POS_CANCEL_AUTHORIZE"),
                                new SimpleGrantedAuthority("POS_REFUND_EXECUTE"),
                                new SimpleGrantedAuthority("POS_CASH_SUPPLY"),
                                new SimpleGrantedAuthority("POS_CASH_WITHDRAWAL"),
                                new SimpleGrantedAuthority("POS_CASH_MOVEMENT_READ"),
                                new SimpleGrantedAuthority("POS_AUTHORIZE_HIGH_WITHDRAWAL"),
                                new SimpleGrantedAuthority("POS_RECEIPT_PRINT"),
                                new SimpleGrantedAuthority("POS_RECEIPT_REPRINT"),
                                new SimpleGrantedAuthority("POS_REPORT_READ"),
                                new SimpleGrantedAuthority("POS_AUDIT_READ"),
                                new SimpleGrantedAuthority("POS_PAYMENT_RECEIVE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_REFUND"),
                                new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                new SimpleGrantedAuthority("PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("CUSTOMER_CREATE"),
                                new SimpleGrantedAuthority("CUSTOMER_READ"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("PRODUCT_CREATE"),
                                new SimpleGrantedAuthority("PRODUCT_READ"),
                                new SimpleGrantedAuthority("POS_MULTI_SESSION"),
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

