package br.com.systemcommerce.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.purchase.dto.SupplierReturnCreateRequest;
import br.com.systemcommerce.purchase.dto.SupplierReturnItemRequest;
import br.com.systemcommerce.purchase.dto.SupplierReturnResponse;
import br.com.systemcommerce.purchase.entity.SupplierReturn;
import br.com.systemcommerce.purchase.mapper.SupplierReturnMapper;
import br.com.systemcommerce.purchase.repository.PurchaseOrderItemRepository;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptItemRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.purchase.repository.SupplierReturnRepository;
import br.com.systemcommerce.purchase.repository.SupplierReturnStatusHistoryRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
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
class SupplierReturnServiceTest {

    @Mock
    private SupplierReturnRepository supplierReturnRepository;

    @Mock
    private SupplierReturnStatusHistoryRepository statusHistoryRepository;

    @Mock
    private StoreSupplierReturnSequenceService storeSupplierReturnSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private PurchaseReceiptRepository purchaseReceiptRepository;

    @Mock
    private PurchaseReceiptItemRepository purchaseReceiptItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private PayableService payableService;

    private SupplierReturnService supplierReturnService;

    private UUID userId;
    private UUID storeId;
    private UUID warehouseId;
    private Store store;
    private Warehouse warehouse;
    private Supplier supplier;
    private Product product;
    private AtomicReference<SupplierReturn> savedReturn;

    @BeforeEach
    void setUp() {
        supplierReturnService = new SupplierReturnService(
                supplierReturnRepository,
                statusHistoryRepository,
                new SupplierReturnMapper(),
                storeSupplierReturnSequenceService,
                storeAuthorizationEvaluator,
                warehouseService,
                productRepository,
                supplierRepository,
                purchaseOrderRepository,
                purchaseOrderItemRepository,
                purchaseReceiptRepository,
                purchaseReceiptItemRepository,
                inventoryService,
                userRepository,
                domainAuditService,
                payableService);

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
        warehouse.setCode("DEP01");
        warehouse.setStore(store);

        supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setLegalName("Fornecedor XPTO");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Parafuso M6");

        savedReturn = new AtomicReference<>();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("SUPPLIER_RETURN_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubPersistence() {
        lenient().when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        lenient().when(warehouseService.requireUsable(warehouseId)).thenReturn(warehouse);
        lenient().when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        lenient().when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        lenient()
                .when(storeSupplierReturnSequenceService.allocateNextReturnNumber(store))
                .thenReturn("DEV-LJ01-000001");
        lenient().when(supplierReturnRepository.save(any(SupplierReturn.class))).thenAnswer(inv -> {
            SupplierReturn r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            r.getItems().forEach(item -> {
                if (item.getId() == null) {
                    item.setId(UUID.randomUUID());
                }
            });
            savedReturn.set(r);
            return r;
        });
        lenient()
                .when(supplierReturnRepository.findDetailedById(any()))
                .thenAnswer(inv -> Optional.ofNullable(savedReturn.get()));
    }

    private SupplierReturnResponse createDraftWithOneItem(BigDecimal quantity) {
        stubPersistence();
        SupplierReturnCreateRequest request = new SupplierReturnCreateRequest(
                storeId,
                warehouseId,
                supplier.getId(),
                null,
                null,
                SupplierReturn.ReturnReason.DAMAGE,
                "Produto avariado no transporte",
                SupplierReturn.OriginType.RECEIPT,
                null,
                List.of(new SupplierReturnItemRequest(
                        product.getId(), null, null, quantity, new BigDecimal("10.00"), null, null, null, null)));
        return supplierReturnService.create(request);
    }

    @Test
    void shouldCreateDraftReturnWithSequentialNumber() {
        SupplierReturnResponse response = createDraftWithOneItem(new BigDecimal("5"));

        assertThat(response.returnNumber()).isEqualTo("DEV-LJ01-000001");
        assertThat(response.status()).isEqualTo(SupplierReturn.SupplierReturnStatus.DRAFT);
        assertThat(savedReturn.get().getItems()).hasSize(1);
    }

    @Test
    void shouldFollowFullApprovalDispatchAndCompleteFlow() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("5"));
        when(inventoryService.availableQuantity(product.getId(), warehouseId)).thenReturn(new BigDecimal("20.000"));

        supplierReturnService.submit(draft.id());
        assertThat(savedReturn.get().getStatus()).isEqualTo(SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL);

        supplierReturnService.approve(draft.id());
        assertThat(savedReturn.get().getStatus()).isEqualTo(SupplierReturn.SupplierReturnStatus.APPROVED);

        supplierReturnService.dispatch(draft.id());
        assertThat(savedReturn.get().getStatus()).isEqualTo(SupplierReturn.SupplierReturnStatus.DISPATCHED);
        assertThat(savedReturn.get().getDispatchedAt()).isNotNull();

        SupplierReturnResponse completed = supplierReturnService.complete(draft.id());
        assertThat(completed.status()).isEqualTo(SupplierReturn.SupplierReturnStatus.COMPLETED);
        assertThat(savedReturn.get().getCompletedAt()).isNotNull();
        verify(inventoryService)
                .registerSupplierReturn(product.getId(), warehouseId, new BigDecimal("5.000"), draft.id());
    }

    @Test
    void shouldNotCompleteWhenAvailableStockIsInsufficient() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("50"));
        when(inventoryService.availableQuantity(product.getId(), warehouseId)).thenReturn(new BigDecimal("3.000"));

        supplierReturnService.submit(draft.id());
        supplierReturnService.approve(draft.id());
        supplierReturnService.dispatch(draft.id());

        assertThatThrownBy(() -> supplierReturnService.complete(draft.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saldo insuficiente");
        verify(inventoryService, never()).registerSupplierReturn(any(), any(), any(), any());
    }

    @Test
    void shouldRejectPendingApprovalWithReason() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("5"));
        supplierReturnService.submit(draft.id());

        assertThatThrownBy(() -> supplierReturnService.reject(draft.id(), null))
                .isInstanceOf(BusinessRuleException.class);

        SupplierReturnResponse rejected = supplierReturnService.reject(draft.id(), "Fornecedor não aceitou devolução");
        assertThat(rejected.status()).isEqualTo(SupplierReturn.SupplierReturnStatus.REJECTED);
    }

    @Test
    void shouldCancelDraftWithReason() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("5"));

        SupplierReturnResponse cancelled = supplierReturnService.cancel(draft.id(), "Item ainda em uso");
        assertThat(cancelled.status()).isEqualTo(SupplierReturn.SupplierReturnStatus.CANCELLED);
    }

    @Test
    void shouldNotCancelAfterCompleted() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("5"));
        when(inventoryService.availableQuantity(product.getId(), warehouseId)).thenReturn(new BigDecimal("20.000"));
        supplierReturnService.submit(draft.id());
        supplierReturnService.approve(draft.id());
        supplierReturnService.dispatch(draft.id());
        supplierReturnService.complete(draft.id());

        assertThatThrownBy(() -> supplierReturnService.cancel(draft.id(), "Motivo qualquer"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldNotSubmitReturnWithoutItems() {
        stubPersistence();
        when(supplierReturnRepository.save(any(SupplierReturn.class))).thenAnswer(inv -> {
            SupplierReturn r = inv.getArgument(0);
            r.getItems().clear();
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            savedReturn.set(r);
            return r;
        });
        SupplierReturnCreateRequest request = new SupplierReturnCreateRequest(
                storeId,
                warehouseId,
                supplier.getId(),
                null,
                null,
                SupplierReturn.ReturnReason.OTHER,
                null,
                SupplierReturn.OriginType.EXISTING_STOCK,
                null,
                List.of(new SupplierReturnItemRequest(
                        product.getId(), null, null, new BigDecimal("1"), null, null, null, null, null)));
        SupplierReturnResponse draft = supplierReturnService.create(request);

        assertThatThrownBy(() -> supplierReturnService.submit(draft.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequireDispatchedStatusBeforeCompleting() {
        SupplierReturnResponse draft = createDraftWithOneItem(new BigDecimal("5"));

        assertThatThrownBy(() -> supplierReturnService.complete(draft.id()))
                .isInstanceOf(BusinessRuleException.class);
        verify(inventoryService, times(0)).registerSupplierReturn(any(), any(), any(), any());
    }
}
