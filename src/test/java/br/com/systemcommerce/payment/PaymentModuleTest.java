package br.com.systemcommerce.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.SaleFinancialSummaryResponse;
import br.com.systemcommerce.payment.dto.SalePaymentBalanceResponse;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
class PaymentModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_payment_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryService inventoryService;

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

        Customer maria = customerRepository.findByDocument("52998224725").orElseThrow();
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
    void shouldRegisterConfirmFullPaymentAndMarkSalePaid() throws Exception {
        SaleResponse sale = confirmedSale(new BigDecimal("100.00"), BigDecimal.ONE);

        PaymentResponse payment = paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.PIX,
                new BigDecimal("100.00"),
                null,
                "TX-FULL",
                null,
                1,
                null,
                true));

        assertThat(payment.status()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(payment.changeAmount()).isEqualByComparingTo("0.00");
        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.PAID);

        SalePaymentBalanceResponse balance = paymentService.balance(sale.id());
        assertThat(balance.balanceDue()).isEqualByComparingTo("0.00");
        assertThat(balance.confirmedPaid()).isEqualByComparingTo("100.00");

        mockMvc.perform(get("/api/v1/payments/by-sale/{saleId}/summary", sale.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullyPaid").value(true))
                .andExpect(jsonPath("$.data.saleStatus").value("PAID"));
    }

    @Test
    void shouldSupportPartialPayments() {
        SaleResponse sale = confirmedSale(new BigDecimal("100.00"), BigDecimal.ONE);

        paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.CASH,
                new BigDecimal("40.00"),
                null,
                null,
                null,
                1,
                new BigDecimal("40.00"),
                true));

        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.PARTIALLY_PAID);

        paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.CREDIT_CARD,
                new BigDecimal("60.00"),
                null,
                null,
                null,
                2,
                null,
                true));

        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.PAID);
        SaleFinancialSummaryResponse summary = paymentService.financialSummary(sale.id());
        assertThat(summary.confirmedPaid()).isEqualByComparingTo("100.00");
        assertThat(summary.balanceDue()).isEqualByComparingTo("0.00");
        assertThat(summary.payments()).hasSize(2);
    }

    @Test
    void shouldCalculateCashChangeOnApi() {
        SaleResponse sale = confirmedSale(new BigDecimal("87.50"), BigDecimal.ONE);

        var change = paymentService.change(sale.id(), new BigDecimal("100.00"));
        assertThat(change.balanceDue()).isEqualByComparingTo("87.50");
        assertThat(change.changeAmount()).isEqualByComparingTo("12.50");

        PaymentResponse payment = paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.CASH,
                new BigDecimal("87.50"),
                null,
                null,
                "pagamento em dinheiro",
                1,
                new BigDecimal("100.00"),
                true));

        assertThat(payment.changeAmount()).isEqualByComparingTo("12.50");
        assertThat(payment.tenderedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectPaymentOnCancelledOrDraftSale() {
        Product product = createProduct(new BigDecimal("50.00"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse draft = prepareSale(product.getId(), BigDecimal.ONE);

        assertThatThrownBy(() -> paymentService.register(new PaymentCreateRequest(
                        draft.id(),
                        Payment.PaymentMethod.PIX,
                        new BigDecimal("50.00"),
                        null,
                        null,
                        null,
                        1,
                        null,
                        false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rascunho");

        SaleResponse confirmed = saleService.confirm(draft.id());
        saleService.cancel(confirmed.id(), new SaleCancelRequest("sem interesse"));

        assertThatThrownBy(() -> paymentService.register(new PaymentCreateRequest(
                        confirmed.id(),
                        Payment.PaymentMethod.PIX,
                        new BigDecimal("50.00"),
                        null,
                        null,
                        null,
                        1,
                        null,
                        false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void shouldRejectOverpaymentAndNegativeAmount() {
        SaleResponse sale = confirmedSale(new BigDecimal("50.00"), BigDecimal.ONE);

        assertThatThrownBy(() -> paymentService.register(new PaymentCreateRequest(
                        sale.id(),
                        Payment.PaymentMethod.PIX,
                        new BigDecimal("80.00"),
                        null,
                        null,
                        null,
                        1,
                        null,
                        true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("excede");
    }

    @Test
    void shouldBeIdempotentOnConfirmAndRefundRestoringFinancialStatus() {
        SaleResponse sale = confirmedSale(new BigDecimal("100.00"), BigDecimal.ONE);

        PaymentResponse pending = paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.TRANSFER,
                new BigDecimal("100.00"),
                null,
                "REF-1",
                null,
                1,
                null,
                false));

        PaymentResponse first = paymentService.confirm(pending.id());
        PaymentResponse second = paymentService.confirm(pending.id());
        assertThat(first.status()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(second.status()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.PAID);

        PaymentResponse refunded = paymentService.refund(pending.id(), new PaymentRefundRequest("estorno teste"));
        PaymentResponse refundedAgain =
                paymentService.refund(pending.id(), new PaymentRefundRequest("estorno teste"));
        assertThat(refunded.status()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(refundedAgain.status()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.CONFIRMED);
        assertThat(paymentRepository.findById(pending.id())).isPresent();
        assertThat(paymentService.statusHistory(pending.id())).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldBlockSaleCancelWhenConfirmedPaymentExists() {
        SaleResponse sale = confirmedSale(new BigDecimal("30.00"), BigDecimal.ONE);
        paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.DEBIT_CARD,
                new BigDecimal("30.00"),
                null,
                null,
                null,
                1,
                null,
                true));

        assertThatThrownBy(() -> saleService.cancel(sale.id(), new SaleCancelRequest("tentativa")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pagamentos confirmados");
    }

    @Test
    void shouldRejectConfirmAfterCancel() {
        SaleResponse sale = confirmedSale(new BigDecimal("20.00"), BigDecimal.ONE);
        PaymentResponse payment = paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.OTHER,
                new BigDecimal("20.00"),
                null,
                null,
                null,
                1,
                null,
                false));
        paymentService.cancel(payment.id(), new PaymentCancelRequest("desisti"));

        assertThatThrownBy(() -> paymentService.confirm(payment.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void shouldProtectConcurrentPaymentsAgainstOverpayment() throws Exception {
        SaleResponse sale = confirmedSale(new BigDecimal("100.00"), BigDecimal.ONE);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable pay = () -> {
            try {
                start.await();
                authenticateAs(adminUserId);
                paymentService.register(new PaymentCreateRequest(
                        sale.id(),
                        Payment.PaymentMethod.PIX,
                        new BigDecimal("60.00"),
                        null,
                        null,
                        null,
                        1,
                        null,
                        true));
                successes.incrementAndGet();
            } catch (Exception ex) {
                failures.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
                done.countDown();
            }
        };
        executor.submit(pay);
        executor.submit(pay);
        start.countDown();
        assertThat(done.await(40, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
        assertThat(paymentService.balance(sale.id()).confirmedPaid()).isEqualByComparingTo("60.00");
    }

    private SaleResponse confirmedSale(BigDecimal unitPrice, BigDecimal quantity) {
        Product product = createProduct(unitPrice);
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("100"), "seed", false));
        SaleResponse sale = prepareSale(product.getId(), quantity);
        return saleService.confirm(sale.id());
    }

    private SaleResponse prepareSale(UUID productId, BigDecimal quantity) {
        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, null));
        if (draft.customerId() == null) {
            saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        }
        return saleService.addItem(
                draft.id(), new SaleItemRequest(productId, quantity, null, BigDecimal.ZERO, null));
    }

    private Product createProduct(BigDecimal salePrice) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("PAY-" + suffix);
        product.setSku("SKU-PAY-" + suffix);
        product.setName("Produto pagamento " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(salePrice);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
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
                        new SimpleGrantedAuthority("SALE_CANCEL"),
                        new SimpleGrantedAuthority("SALE_READ"),
                        new SimpleGrantedAuthority("PAYMENT_MANAGE"),
                        new SimpleGrantedAuthority("INVENTORY_MOVE"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
