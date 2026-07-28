package br.com.systemcommerce.pos.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashMovementService;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
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
import org.springframework.data.domain.Pageable;
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
class PosCheckoutModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_checkout_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private CashMovementService cashMovementService;

    @Autowired
    private CashMovementRepository cashMovementRepository;

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

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withCheckoutSecurity(() -> {
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
                        "cleanup-" + UUID.randomUUID());
            });

            var session = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("200.00"), null),
                    "checkout-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldFinalizeIntegralPayment() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            seedStock(product.getId(), "10");
            var sale = saleWithItem(product, BigDecimal.ONE);

            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("100.00"),
                            null,
                            1,
                            "PIX-1",
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    "pay-full-" + sale.id());

            var finalized = posCheckoutService.finalizeSale(sale.id(), "fin-full-" + sale.id());
            assertThat(finalized.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(finalized.balanceDue()).isEqualByComparingTo("0.00");
            assertThat(finalized.receiptNumber()).isEqualTo(finalized.sale().saleNumber());
            assertThat(finalized.printData()).isNotNull();
            assertThat(finalized.payments()).hasSize(1);
            assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.PAID);
        });
    }

    @Test
    void shouldSupportPartialThenComplete() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            seedStock(product.getId(), "10");
            var sale = saleWithItem(product, BigDecimal.ONE);

            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("40.00"),
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);

            var status = posCheckoutService.finalizeStatus(sale.id());
            assertThat(status.readyToFinalize()).isFalse();

            assertThatThrownBy(() -> posCheckoutService.finalizeSale(sale.id(), null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("insuficiente");

            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isEqualTo(Sale.SaleStatus.DRAFT);

            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.DEBIT_CARD,
                            new BigDecimal("60.00"),
                            null,
                            1,
                            null,
                            null,
                            "AUTH1",
                            "NSU1",
                            "VISA",
                            "CIELO",
                            false),
                    null);

            var finalized = posCheckoutService.finalizeSale(sale.id(), null);
            assertThat(finalized.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(finalized.payments()).hasSize(2);
        });
    }

    @Test
    void shouldSupportMultipleMethodsWithCashChange() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            seedStock(product.getId(), "10");
            var sale = saleWithItem(product, BigDecimal.ONE);

            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("30.00"), null, 1, null, null, null, null, null, null, false),
                    null);
            var cash = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("70.00"),
                            new BigDecimal("100.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);

            assertThat(cash.appliedAmount()).isEqualByComparingTo("70.00");
            assertThat(cash.changeAmount()).isEqualByComparingTo("30.00");
            assertThat(cash.tenderedAmount()).isEqualByComparingTo("100.00");

            var changePreview = posCheckoutService.change(sale.id(), new BigDecimal("50.00"));
            assertThat(changePreview.changeAmount()).isEqualByComparingTo("0.00");

            var finalized = posCheckoutService.finalizeSale(sale.id(), null);
            assertThat(finalized.changeTotal()).isEqualByComparingTo("30.00");
            assertThat(finalized.status()).isEqualTo(Sale.SaleStatus.PAID);

            BigDecimal cashSales = cashMovementService.physicalBalance(cashSessionId).cashSales();
            assertThat(cashSales).isEqualByComparingTo("70.00");
        });
    }

    @Test
    void shouldRejectInsufficientOnFinalize() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("80.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("20.00"), null, 1, null, null, null, null, null, null, false),
                    null);

            assertThatThrownBy(() -> posCheckoutService.finalizeSale(sale.id(), null))
                    .isInstanceOf(BusinessRuleException.class);

            assertThat(saleRepository.findById(sale.id()).orElseThrow().isDraft()).isTrue();
            assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(sale.id()).getFirst().isPending())
                    .isTrue();
        });
    }

    @Test
    void shouldAvoidDuplicatePaymentByIdempotency() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("50.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            String key = "dup-pay-" + sale.id();

            var first = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("50.00"), null, 1, null, null, null, null, null, null, false),
                    key);
            var second = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("50.00"), null, 1, null, null, null, null, null, null, false),
                    key);

            assertThat(first.id()).isEqualTo(second.id());
            assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(sale.id())).hasSize(1);
        });
    }

    @Test
    void shouldNotMarkSalePaidWhenPaymentRefused() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("45.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            var payment = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CREDIT_CARD,
                            new BigDecimal("45.00"),
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);

            var refused = posCheckoutService.refuse(
                    sale.id(), payment.id(), new PaymentCancelRequest("cartão recusado"));
            assertThat(refused.status()).isEqualTo(Payment.PaymentStatus.CANCELLED);

            assertThatThrownBy(() -> posCheckoutService.finalizeSale(sale.id(), null))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isNotEqualTo(Sale.SaleStatus.PAID);
            assertThat(paymentRepository.findById(payment.id()).orElseThrow().isCancelled()).isTrue();
        });
    }

    @Test
    void shouldRefundConfirmedPayment() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("55.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("55.00"),
                            new BigDecimal("55.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);
            var finalized = posCheckoutService.finalizeSale(sale.id(), null);
            UUID paymentId = finalized.payments().getFirst().id();

            var refunded = posCheckoutService.refund(
                    sale.id(), paymentId, new PaymentRefundRequest("cliente desistiu"));
            assertThat(refunded.status()).isEqualTo(Payment.PaymentStatus.REFUNDED);
            assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.CONFIRMED);

            boolean hasRefund = cashMovementRepository
                    .findByCashSessionIdOrderByOccurredAtAsc(cashSessionId)
                    .stream()
                    .anyMatch(m -> m.getType() == CashMovement.MovementType.CASH_REFUND);
            assertThat(hasRefund).isTrue();
        });
    }

    @Test
    void shouldBeIdempotentOnDuplicateFinalize() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("33.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("33.00"), null, 1, null, null, null, null, null, null, false),
                    null);
            String key = "fin-dup-" + sale.id();
            var first = posCheckoutService.finalizeSale(sale.id(), key);
            var second = posCheckoutService.finalizeSale(sale.id(), key);
            assertThat(first.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(second.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(first.receiptNumber()).isEqualTo(second.receiptNumber());
            assertThat(paymentRepository.countBySaleIdAndStatus(sale.id(), Payment.PaymentStatus.CONFIRMED))
                    .isEqualTo(1);
        });
    }

    @Test
    void shouldRollbackFinalizeWhenCoverageFailsAfterConfirmAttempt() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("90.00"));
            seedStock(product.getId(), "10");
            BigDecimal stockBefore = inventoryService.availableQuantity(product.getId(), warehouseId);

            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("10.00"), null, 1, null, null, null, null, null, null, false),
                    null);

            assertThatThrownBy(() -> posCheckoutService.finalizeSale(sale.id(), "rb-" + sale.id()))
                    .isInstanceOf(BusinessRuleException.class);

            Sale after = saleRepository.findById(sale.id()).orElseThrow();
            assertThat(after.isDraft()).isTrue();
            assertThat(paymentRepository.findBySaleIdOrderByCreatedAtAsc(sale.id()).getFirst().isPending())
                    .isTrue();
            BigDecimal stockAfter = inventoryService.availableQuantity(product.getId(), warehouseId);
            assertThat(stockAfter).isEqualByComparingTo(stockBefore);
        });
    }

    @Test
    void shouldRejectPaymentAfterPaidOrCancelled() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("25.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, new BigDecimal("25.00"), null, 1, null, null, null, null, null, null, false),
                    null);
            posCheckoutService.finalizeSale(sale.id(), null);

            assertThatThrownBy(() -> posCheckoutService.addPayment(
                            sale.id(),
                            new PosPaymentAddRequest(
                                    Payment.PaymentMethod.PIX,
                                    new BigDecimal("1.00"),
                                    null,
                                    1,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false),
                            null))
                    .isInstanceOf(BusinessRuleException.class);
        });
    }

    @Test
    void shouldRemovePendingPayment() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("40.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            var payment = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("40.00"),
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);

            var removed = posCheckoutService.removePending(sale.id(), payment.id());
            assertThat(removed.status()).isEqualTo(Payment.PaymentStatus.CANCELLED);
            assertThat(posCheckoutService.listPayments(sale.id())).hasSize(1);
            assertThat(posCheckoutService.listPayments(sale.id()).getFirst().status())
                    .isEqualTo(Payment.PaymentStatus.CANCELLED);
        });
    }

    @Test
    void shouldRefusePendingPaymentViaCheckout() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("48.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            var payment = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CREDIT_CARD,
                            new BigDecimal("48.00"),
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);

            var refused = posCheckoutService.refuse(
                    sale.id(), payment.id(), new PaymentCancelRequest("TEF recusado"));
            assertThat(refused.status()).isEqualTo(Payment.PaymentStatus.CANCELLED);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isNotEqualTo(Sale.SaleStatus.PAID);
        });
    }

    @Test
    void shouldRejectPaymentOnCancelledSale() {
        withCheckoutSecurity(() -> {
            Product product = createProduct(new BigDecimal("22.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posSaleService.discard(
                    sale.id(),
                    new br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest("cancelada no teste", sale.version()),
                    null);

            assertThatThrownBy(() -> posCheckoutService.addPayment(
                            sale.id(),
                            new PosPaymentAddRequest(
                                    Payment.PaymentMethod.PIX,
                                    new BigDecimal("22.00"),
                                    null,
                                    1,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false),
                            null))
                    .isInstanceOf(BusinessRuleException.class);
        });
    }

    private br.com.systemcommerce.sale.dto.SaleResponse saleWithItem(Product product, BigDecimal qty) {
        var sale = posSaleService.start(new PosSaleStartRequest(cashSessionId), "sale-" + UUID.randomUUID());
        return posSaleService.addByProductId(
                sale.id(),
                new PosSaleAddByProductIdRequest(product.getId(), qty, sale.version()),
                null);
    }

    private Product createProduct(BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("CHK-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto checkout " + product.getSku());
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
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed checkout", false));
    }

    private void withCheckoutSecurity(Runnable action) {
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
                                new SimpleGrantedAuthority("POS_SALE_SUSPEND"),
                                new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_REFUND"),
                                new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
