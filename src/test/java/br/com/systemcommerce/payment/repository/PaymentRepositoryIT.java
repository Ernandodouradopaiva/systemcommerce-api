package br.com.systemcommerce.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
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
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Testes de queries do PaymentRepository com PostgreSQL real (sem mock). */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PaymentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_payment_repo_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SaleService saleService;

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

    private UUID customerId;
    private UUID loja01Id;
    private UUID dep01Id;
    private Category category;

    @BeforeEach
    void setUp() {
        UUID adminUserId = UUID.fromString("a0000000-0000-4000-8000-000000000001");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("PAYMENT_MANAGE"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));

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
    void sumConfirmedAmountShouldIgnorePendingAndCancelled() {
        Product product = createProduct(new BigDecimal("100.00"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse sale = prepareConfirmedSale(product.getId(), BigDecimal.ONE);

        paymentService.register(new PaymentCreateRequest(
                sale.id(), Payment.PaymentMethod.PIX, new BigDecimal("40.00"), null, null, null, 1, null, true));
        var pending = paymentService.register(new PaymentCreateRequest(
                sale.id(), Payment.PaymentMethod.CASH, new BigDecimal("30.00"), null, null, null, 2, null, false));
        var cancelled = paymentService.register(new PaymentCreateRequest(
                sale.id(),
                Payment.PaymentMethod.TRANSFER,
                new BigDecimal("20.00"),
                null,
                null,
                null,
                3,
                null,
                true));
        paymentService.refund(cancelled.id(), new PaymentRefundRequest("estorno teste"));

        assertThat(paymentRepository.sumConfirmedAmountBySaleId(sale.id())).isEqualByComparingTo("40.00");
        assertThat(paymentRepository.hasConfirmedPayments(sale.id())).isTrue();
        assertThat(pending.status()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    private SaleResponse prepareConfirmedSale(UUID productId, BigDecimal quantity) {
        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, null));
        if (draft.customerId() == null) {
            saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        }
        saleService.addItem(
                draft.id(), new SaleItemRequest(productId, quantity, null, BigDecimal.ZERO, null));
        return saleService.confirm(draft.id());
    }

    private Product createProduct(BigDecimal salePrice) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("PREP-" + suffix);
        product.setSku("SKU-PREP-" + suffix);
        product.setName("Produto repo " + suffix);
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
}
