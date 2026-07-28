package br.com.systemcommerce.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida rollback transacional real no PostgreSQL: se a auditoria falhar após a baixa de estoque,
 * a confirmação não deve persistir (estoque e status voltam ao estado anterior).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SaleTransactionRollbackTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_sale_tx_test")
            .withUsername("test")
            .withPassword("test");

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

    @MockitoSpyBean
    private DomainAuditService domainAuditService;

    private UUID adminUserId;
    private UUID customerId;
    private UUID loja01Id;
    private UUID dep01Id;
    private Category category;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.fromString("a0000000-0000-4000-8000-000000000001");
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
    void shouldRollbackStockWhenAuditFailsDuringConfirm() {
        Product product = createProduct();
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, null));
        if (draft.customerId() == null) {
            saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        }
        saleService.addItem(
                draft.id(),
                new SaleItemRequest(product.getId(), new BigDecimal("3"), null, BigDecimal.ZERO, null));

        doThrow(new RuntimeException("falha forçada na auditoria"))
                .when(domainAuditService)
                .record(anyString(), any(), any(), any(), any(), anyString());

        assertThatThrownBy(() -> saleService.confirm(draft.id())).isInstanceOf(RuntimeException.class);

        assertThat(saleService.getById(draft.id()).status()).isEqualTo(Sale.SaleStatus.DRAFT);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("10");
    }

    private Product createProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("TX-" + suffix);
        product.setSku("SKU-TX-" + suffix);
        product.setName("Produto tx " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(new BigDecimal("10.00"));
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        return saved;
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
    }
}
