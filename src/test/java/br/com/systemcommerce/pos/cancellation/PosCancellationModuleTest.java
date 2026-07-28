package br.com.systemcommerce.pos.cancellation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cancellation.dto.CancellationDecisionRequest;
import br.com.systemcommerce.pos.cancellation.dto.CancellationRequestCreate;
import br.com.systemcommerce.pos.cancellation.dto.SaleReturnCreateRequest;
import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.service.PosCancellationService;
import br.com.systemcommerce.pos.cancellation.service.PosReturnService;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
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
class PosCancellationModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_cancel_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosCancellationService posCancellationService;

    @Autowired
    private PosReturnService posReturnService;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

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
    private SaleRepository saleRepository;

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

        withFullSecurity(() -> {
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
                    new CashSessionOpenRequest(terminalId, new BigDecimal("300.00"), null),
                    "cancel-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldCancelDraftAndBeIdempotent() {
        withFullSecurity(() -> {
            Product product = createProduct(new BigDecimal("40.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            String key = "cancel-draft-" + sale.id();

            var first = posCancellationService.request(
                    new CancellationRequestCreate(sale.id(), "cliente desistiu"), key);
            var second = posCancellationService.request(
                    new CancellationRequestCreate(sale.id(), "cliente desistiu"), key);

            assertThat(first.id()).isEqualTo(second.id());
            assertThat(first.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isEqualTo(Sale.SaleStatus.CANCELLED);
        });
    }

    @Test
    void shouldRestoreStockAndReverseCashOnCompletedCancel() {
        withFullSecurity(() -> {
            Product product = createProduct(new BigDecimal("80.00"));
            seedStock(product.getId(), "10");
            BigDecimal stockBefore = inventoryService.availableQuantity(product.getId(), warehouseId);
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("80.00"),
                            new BigDecimal("80.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);
            posCheckoutService.finalizeSale(sale.id(), null);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo(stockBefore.subtract(BigDecimal.ONE));

            var requested = posCancellationService.request(
                    new CancellationRequestCreate(sale.id(), "erro de operação"), null);
            assertThat(requested.status()).isEqualTo(SaleCancellation.Status.REQUESTED);

            posCancellationService.authorize(requested.id(), new CancellationDecisionRequest("ok gerente"));
            var executed = posCancellationService.execute(requested.id());

            assertThat(executed.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(executed.authorizedById()).isNotNull();
            assertThat(executed.executedById()).isNotNull();
            assertThat(executed.refunds()).isNotEmpty();
            assertThat(executed.refunds().getFirst().status()).isEqualTo(CancellationRefund.Status.COMPLETED);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isEqualTo(Sale.SaleStatus.CANCELLED);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo(stockBefore);

            boolean hasCashRefund = cashMovementRepository
                    .findByCashSessionIdOrderByOccurredAtAsc(cashSessionId)
                    .stream()
                    .anyMatch(m -> m.getType() == CashMovement.MovementType.CASH_REFUND);
            assertThat(hasCashRefund).isTrue();
        });
    }

    @Test
    void shouldRefundElectronicPaymentOnCancel() {
        withFullSecurity(() -> {
            Product product = createProduct(new BigDecimal("60.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("60.00"),
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
            posCheckoutService.finalizeSale(sale.id(), null);

            var requested = posCancellationService.request(
                    new CancellationRequestCreate(sale.id(), "estorno eletrônico"), null);
            posCancellationService.authorize(requested.id(), null);
            var executed = posCancellationService.execute(requested.id());

            assertThat(executed.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(executed.refunds().getFirst().method()).isEqualTo(Payment.PaymentMethod.PIX);
            assertThat(executed.refunds().getFirst().status()).isEqualTo(CancellationRefund.Status.COMPLETED);
        });
    }

    @Test
    void shouldHandlePartialFailureAndReprocess() {
        withFullSecurity(() -> {
            Product product = createProduct(new BigDecimal("90.00"));
            seedStock(product.getId(), "5");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            new BigDecimal("90.00"),
                            new BigDecimal("90.00"),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    null);
            posCheckoutService.finalizeSale(sale.id(), null);

            var requested = posCancellationService.request(
                    new CancellationRequestCreate(sale.id(), "falha parcial"), null);
            posCancellationService.authorize(requested.id(), null);

            // fecha caixa para falhar CASH_REFUND
            var recon = cashSessionService.reconcile(cashSessionId);
            cashSessionService.close(
                    cashSessionId, new CashSessionCloseRequest(recon.expectedCash(), "force fail"), "close-fail");

            var failed = posCancellationService.execute(requested.id());
            assertThat(failed.status()).isEqualTo(SaleCancellation.Status.PARTIALLY_FAILED);
            assertThat(failed.refunds().getFirst().status()).isEqualTo(CancellationRefund.Status.FAILED);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().getStatus())
                    .isNotEqualTo(Sale.SaleStatus.CANCELLED);

            // reabre caixa e reprocessa
            var reopened = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("300.00"), null),
                    "reopen-" + UUID.randomUUID());
            cashSessionId = reopened.id();

            var reprocessed = posCancellationService.reprocessRefund(
                    failed.id(), failed.refunds().getFirst().id());
            assertThat(reprocessed.status()).isEqualTo(SaleCancellation.Status.COMPLETED);
            assertThat(reprocessed.refunds().getFirst().status()).isEqualTo(CancellationRefund.Status.COMPLETED);
            assertThat(saleRepository.findById(sale.id()).orElseThrow().isCancelled()).isTrue();
        });
    }

    @Test
    void shouldDenyAccessWithoutPermission() {
        Product product = createProduct(new BigDecimal("25.00"));
        seedStock(product.getId(), "3");
        final UUID[] saleId = new UUID[1];
        withFullSecurity(() -> saleId[0] = saleWithItem(product, BigDecimal.ONE).id());

        withNoCancelSecurity(() -> assertThatThrownBy(() -> posCancellationService.request(
                        new CancellationRequestCreate(saleId[0], "sem permissão"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permissão"));
    }

    @Test
    void shouldRegisterFutureReturnDocument() {
        withFullSecurity(() -> {
            Product product = createProduct(new BigDecimal("35.00"));
            seedStock(product.getId(), "8");
            var sale = saleWithItem(product, BigDecimal.ONE);
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            new BigDecimal("35.00"),
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
            posCheckoutService.finalizeSale(sale.id(), null);
            BigDecimal stockBefore = inventoryService.availableQuantity(product.getId(), warehouseId);

            var ret = posReturnService.register(
                    new SaleReturnCreateRequest(
                            sale.id(),
                            cashSessionId,
                            "produto com defeito",
                            null,
                            List.of(new SaleReturnCreateRequest.SaleReturnItemRequest(
                                    product.getId(), sale.items().getFirst().id(), BigDecimal.ONE))),
                    "ret-" + sale.id());

            assertThat(ret.returnNumber()).startsWith("DEV-");
            assertThat(ret.items()).hasSize(1);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo(stockBefore.add(BigDecimal.ONE));

            var again = posReturnService.register(
                    new SaleReturnCreateRequest(
                            sale.id(),
                            cashSessionId,
                            "produto com defeito",
                            null,
                            List.of(new SaleReturnCreateRequest.SaleReturnItemRequest(
                                    product.getId(), sale.items().getFirst().id(), BigDecimal.ONE))),
                    "ret-" + sale.id());
            assertThat(again.id()).isEqualTo(ret.id());
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
        product.setInternalCode("CNL-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-C-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto cancel " + product.getSku());
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
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed cancel", false));
    }

    private void withFullSecurity(Runnable action) {
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
                                new SimpleGrantedAuthority("POS_CANCEL_DRAFT"),
                                new SimpleGrantedAuthority("POS_CANCEL_COMPLETED_SALE"),
                                new SimpleGrantedAuthority("POS_CANCEL_AUTHORIZE"),
                                new SimpleGrantedAuthority("POS_REFUND_EXECUTE"),
                                new SimpleGrantedAuthority("POS_RETURN_CREATE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("POS_PAYMENT_REFUND"),
                                new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("SALE_CANCEL"),
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

    private void withNoCancelSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
