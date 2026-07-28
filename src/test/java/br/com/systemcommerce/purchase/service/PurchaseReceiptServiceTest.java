package br.com.systemcommerce.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.purchase.dto.GoodsReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptInspectionRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptResponse;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.mapper.PurchaseReceiptMapper;
import br.com.systemcommerce.purchase.repository.InventoryEntryReferenceRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptDivergenceRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptStatusHistoryRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PurchaseReceiptServiceTest {

    @Mock
    private PurchaseReceiptRepository purchaseReceiptRepository;

    @Mock
    private PurchaseReceiptStatusHistoryRepository statusHistoryRepository;

    @Mock
    private PurchaseReceiptDivergenceRepository divergenceRepository;

    @Mock
    private InventoryEntryReferenceRepository inventoryEntryReferenceRepository;

    @Mock
    private PurchaseReceiptMapper purchaseReceiptMapper;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private PayableService payableService;

    private PurchaseReceiptService purchaseReceiptService;

    private UUID userId;
    private UUID storeId;
    private UUID warehouseId;
    private UUID orderId;
    private UUID orderItemId;
    private Store store;
    private Warehouse warehouse;
    private PurchaseOrder order;
    private PurchaseOrderItem orderItem;
    private Product product;
    private AtomicReference<PurchaseReceipt> savedReceipt;

    @BeforeEach
    void setUp() {
        purchaseReceiptService = new PurchaseReceiptService(
                purchaseReceiptRepository,
                statusHistoryRepository,
                divergenceRepository,
                inventoryEntryReferenceRepository,
                purchaseReceiptMapper,
                purchaseOrderService,
                inventoryService,
                storeAuthorizationEvaluator,
                userRepository,
                domainAuditService,
                payableService);

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

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

        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setLegalName("Fornecedor XYZ");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Produto Rec");

        orderItem = new PurchaseOrderItem();
        orderItem.setId(orderItemId);
        orderItem.setProduct(product);
        orderItem.setLineNumber(1);
        orderItem.setQuantityOrdered(new BigDecimal("10.0000"));
        orderItem.setQuantityReceived(BigDecimal.ZERO);
        orderItem.setUnitCost(new BigDecimal("5.0000"));
        orderItem.setDiscountAmount(BigDecimal.ZERO);
        orderItem.setTaxAmount(BigDecimal.ZERO);
        orderItem.setLineTotal(new BigDecimal("50.00"));

        order = new PurchaseOrder();
        order.setId(orderId);
        order.setOrganization(org);
        order.setStore(store);
        order.setWarehouse(warehouse);
        order.setSupplier(supplier);
        order.setOrderNumber("C-LJ01-000010");
        order.setStatus(PurchaseOrder.PurchaseOrderStatus.APPROVED);
        order.setAllowOverReceipt(Boolean.FALSE);
        order.addItem(orderItem);

        savedReceipt = new AtomicReference<>();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("PURCHASE_RECEIPT_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubPersistence() {
        when(purchaseReceiptRepository.countByNumberPrefix(any(), any())).thenReturn(0L);
        when(purchaseReceiptRepository.save(any(PurchaseReceipt.class))).thenAnswer(inv -> {
            PurchaseReceipt r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            savedReceipt.set(r);
            return r;
        });
        when(purchaseReceiptRepository.findDetailedById(any()))
                .thenAnswer(inv -> Optional.ofNullable(savedReceipt.get()));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(purchaseReceiptMapper.toResponse(any(PurchaseReceipt.class))).thenAnswer(this::toStubResponse);
    }

    private PurchaseReceiptResponse toStubResponse(org.mockito.invocation.InvocationOnMock inv) {
        PurchaseReceipt r = inv.getArgument(0);
        return new PurchaseReceiptResponse(
                r.getId(),
                r.getReceiptNumber(),
                r.getOrganization() != null ? r.getOrganization().getId() : null,
                r.getStore() != null ? r.getStore().getId() : null,
                r.getStore() != null ? r.getStore().getCode() : null,
                r.getWarehouse() != null ? r.getWarehouse().getId() : null,
                r.getWarehouse() != null ? r.getWarehouse().getCode() : null,
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getId() : null,
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getOrderNumber() : null,
                r.getSupplier() != null ? r.getSupplier().getId() : null,
                r.getSupplier() != null ? r.getSupplier().getLegalName() : null,
                r.getReceiptDate(),
                r.getInvoiceNumber(),
                r.getInvoiceSeries(),
                r.getAccessKey(),
                r.getInvoiceIssuedAt(),
                r.getCarrierName(),
                r.getNotes(),
                r.getStatus(),
                null,
                null,
                r.getPostedAt(),
                null,
                List.of(),
                r.isInspectable(),
                r.isAcceptable(),
                r.isPostable(),
                r.isInspectable(),
                r.isCancellable(),
                r.getVersion(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    @Test
    void shouldConfirmPartialReceiptUpdateOrderAndStock() {
        when(purchaseOrderService.requireAccessible(orderId)).thenReturn(order);
        stubPersistence();
        when(inventoryService.registerPurchase(eq(product.getId()), eq(warehouseId), any(), any()))
                .thenReturn(null);

        PurchaseReceiptCreateRequest request = new PurchaseReceiptCreateRequest(
                orderId,
                LocalDate.now(),
                "NF-100",
                null,
                List.of(new PurchaseReceiptItemRequest(
                        orderItemId,
                        new BigDecimal("4.0000"),
                        new BigDecimal("1.0000"),
                        null,
                        null)));

        purchaseReceiptService.createAndConfirm(request);

        assertThat(orderItem.getQuantityReceived()).isEqualByComparingTo("4.0000");
        verify(inventoryService)
                .registerPurchase(eq(product.getId()), eq(warehouseId), eq(new BigDecimal("4.0000")), any());
        verify(purchaseOrderService).applyReceiptProgress(order);
        assertThat(savedReceipt.get().getStatus()).isEqualTo(PurchaseReceipt.PurchaseReceiptStatus.POSTED_TO_INVENTORY);
        assertThat(savedReceipt.get().getItems().getFirst().getQuantityRejected()).isEqualByComparingTo("1.0000");
    }

    @Test
    void shouldRejectOverReceiptBeyondOrderedQuantity() {
        when(purchaseOrderService.requireAccessible(orderId)).thenReturn(order);

        PurchaseReceiptCreateRequest request = new PurchaseReceiptCreateRequest(
                orderId,
                LocalDate.now(),
                null,
                null,
                List.of(new PurchaseReceiptItemRequest(
                        orderItemId, new BigDecimal("11.0000"), BigDecimal.ZERO, null, null)));

        assertThatThrownBy(() -> purchaseReceiptService.createAndConfirm(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("excede o saldo");

        verify(inventoryService, never()).registerPurchase(any(), any(), any(), any());
        verify(purchaseOrderService, never()).applyReceiptProgress(any());
    }

    @Test
    void shouldMarkOrderReceivedWhenFullyDelivered() {
        orderItem.setQuantityReceived(new BigDecimal("6.0000"));
        when(purchaseOrderService.requireAccessible(orderId)).thenReturn(order);
        stubPersistence();
        when(inventoryService.registerPurchase(any(), any(), any(), any())).thenReturn(null);

        purchaseReceiptService.createAndConfirm(new PurchaseReceiptCreateRequest(
                orderId,
                LocalDate.now(),
                null,
                null,
                List.of(new PurchaseReceiptItemRequest(
                        orderItemId, new BigDecimal("4.0000"), BigDecimal.ZERO, null, null))));

        assertThat(orderItem.getQuantityReceived()).isEqualByComparingTo("10.0000");
        verify(purchaseOrderService).applyReceiptProgress(order);
    }

    @Test
    void shouldPostOnlyAcceptedQuantityAfterInspectionDivergence() {
        when(purchaseOrderService.requireAccessible(orderId)).thenReturn(order);
        stubPersistence();
        when(inventoryService.registerPurchase(any(), any(), any(), any())).thenReturn(null);

        PurchaseReceiptResponse draft = purchaseReceiptService.createDraft(new GoodsReceiptCreateRequest(
                orderId,
                LocalDate.now(),
                "NF-200",
                null,
                null,
                null,
                null,
                null,
                List.of(new GoodsReceiptItemRequest(
                        orderItemId, new BigDecimal("10.0000"), BigDecimal.ZERO, null, null, null, null))));

        PurchaseReceipt receiptEntity = savedReceipt.get();
        UUID receiptItemId = receiptEntity.getItems().getFirst().getId() != null
                ? receiptEntity.getItems().getFirst().getId()
                : UUID.randomUUID();
        receiptEntity.getItems().getFirst().setId(receiptItemId);

        purchaseReceiptService.inspect(
                draft.id(),
                new GoodsReceiptInspectionRequest(List.of(new GoodsReceiptInspectionRequest.GoodsReceiptItemInspection(
                        receiptItemId, new BigDecimal("8.0000"), "Avaria parcial", "QUANTITY"))));

        assertThat(receiptEntity.getStatus()).isEqualTo(PurchaseReceipt.PurchaseReceiptStatus.UNDER_INSPECTION);
        verify(divergenceRepository).save(any());

        purchaseReceiptService.accept(draft.id());
        assertThat(receiptEntity.getStatus()).isEqualTo(PurchaseReceipt.PurchaseReceiptStatus.PARTIALLY_ACCEPTED);

        purchaseReceiptService.postToInventory(draft.id(), "idem-key-1");

        verify(inventoryService)
                .registerPurchase(eq(product.getId()), eq(warehouseId), eq(new BigDecimal("8.0000")), any());
        assertThat(orderItem.getQuantityReceived()).isEqualByComparingTo("8.0000");
        assertThat(receiptEntity.getStatus()).isEqualTo(PurchaseReceipt.PurchaseReceiptStatus.POSTED_TO_INVENTORY);
    }

    @Test
    void shouldBeIdempotentOnDoublePostToInventory() {
        when(purchaseOrderService.requireAccessible(orderId)).thenReturn(order);
        stubPersistence();
        when(inventoryService.registerPurchase(any(), any(), any(), any())).thenReturn(null);

        PurchaseReceiptResponse draft = purchaseReceiptService.createDraft(new GoodsReceiptCreateRequest(
                orderId,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(new GoodsReceiptItemRequest(
                        orderItemId, new BigDecimal("5.0000"), BigDecimal.ZERO, null, null, null, null))));
        purchaseReceiptService.accept(draft.id());
        purchaseReceiptService.postToInventory(draft.id(), "idem-key-2");
        purchaseReceiptService.postToInventory(draft.id(), "idem-key-2");

        verify(inventoryService, times(1)).registerPurchase(any(), any(), any(), any());
        verify(purchaseOrderService, times(1)).applyReceiptProgress(any());
    }
}
