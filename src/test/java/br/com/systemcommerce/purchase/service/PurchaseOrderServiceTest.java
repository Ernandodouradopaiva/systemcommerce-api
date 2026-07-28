package br.com.systemcommerce.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.purchase.dto.PurchaseOrderCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.mapper.PurchaseOrderMapper;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.purchase.repository.PurchaseOrderStatusHistoryRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.service.SupplierService;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    @Mock
    private StorePurchaseOrderSequenceService storePurchaseOrderSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private SupplierService supplierService;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private PurchaseQuotationRepository purchaseQuotationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private UUID userId;
    private UUID storeId;
    private UUID warehouseId;
    private Store store;
    private Warehouse warehouse;
    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();

        Organization org = new Organization();
        org.setId(UUID.randomUUID());

        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(org);

        warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setCode("DEP-01");
        warehouse.setStore(store);

        supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setLegalName("Fornecedor ABC");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Produto Compra");
        product.setSalePrice(new BigDecimal("10.00"));

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("PURCHASE_ORDER_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreatePurchaseOrderWithCalculatedTotalsIncludingTax() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(supplierService.requireUsableForPurchase(supplier.getId())).thenReturn(supplier);
        when(warehouseService.requireUsable(warehouseId)).thenReturn(warehouse);
        when(storePurchaseOrderSequenceService.allocateNextOrderNumber(store)).thenReturn("C-LJ01-000001");
        when(productService.requireUsableForSale(product.getId())).thenReturn(product);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        when(purchaseOrderMapper.toResponse(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder o = inv.getArgument(0);
            return new PurchaseOrderResponse(
                    o.getId(),
                    o.getOrderNumber(),
                    store.getOrganization().getId(),
                    storeId,
                    "LJ01",
                    storeId,
                    "LJ01",
                    warehouseId,
                    "DEP-01",
                    supplier.getId(),
                    supplier.getLegalName(),
                    null,
                    null,
                    null,
                    null,
                    o.getStatus(),
                    null,
                    o.getIssuedAt(),
                    null,
                    null,
                    null,
                    null,
                    o.getSubtotalAmount(),
                    o.getDiscountAmount(),
                    o.getFreightAmount(),
                    o.getTaxAmount(),
                    o.getInsuranceAmount(),
                    o.getExpenseAmount(),
                    o.getTotalAmount(),
                    o.getRevisionNumber(),
                    o.getApprovalRequired(),
                    o.getApprovalThresholdAmount(),
                    o.getAllowOverReceipt(),
                    List.of(),
                    true,
                    false,
                    true,
                    true,
                    false,
                    0L,
                    null,
                    null);
        });

        // 2 * 50 = 100; discount header 5; freight 10; tax 8 → total = 100 - 5 + 10 + 8 = 113
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(
                storeId,
                null,
                warehouseId,
                supplier.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                new BigDecimal("8.00"),
                null,
                null,
                null,
                null,
                List.of(new PurchaseOrderItemRequest(
                        product.getId(),
                        new BigDecimal("2"),
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null)));

        PurchaseOrderResponse response = purchaseOrderService.create(request);

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(captor.capture());
        PurchaseOrder saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PurchaseOrder.PurchaseOrderStatus.DRAFT);
        assertThat(saved.getOrderNumber()).isEqualTo("C-LJ01-000001");
        assertThat(saved.getDestinationStore()).isEqualTo(store);
        assertThat(saved.getSubtotalAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(saved.getFreightAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.getTaxAmount()).isEqualByComparingTo("8.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("113.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("113.00");
    }

    @Test
    void shouldRequireApprovalWhenTotalExceedsThreshold() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(supplierService.requireUsableForPurchase(supplier.getId())).thenReturn(supplier);
        when(warehouseService.requireUsable(warehouseId)).thenReturn(warehouse);
        when(storePurchaseOrderSequenceService.allocateNextOrderNumber(store)).thenReturn("C-LJ01-000002");
        when(productService.requireUsableForSale(product.getId())).thenReturn(product);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        when(purchaseOrderMapper.toResponse(any(PurchaseOrder.class))).thenAnswer(inv -> null);

        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(
                storeId,
                null,
                warehouseId,
                supplier.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                new BigDecimal("50.00"),
                null,
                List.of(new PurchaseOrderItemRequest(
                        product.getId(),
                        new BigDecimal("2"),
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null)));

        purchaseOrderService.create(request);

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(captor.capture());
        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getApprovalRequired()).isTrue();
    }
}
