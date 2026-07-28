package br.com.systemcommerce.inventorycount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.entity.InventoryAdjustmentReason;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.inventorycount.dto.InventoryCountActionRequest;
import br.com.systemcommerce.inventorycount.entity.InventoryCount;
import br.com.systemcommerce.inventorycount.entity.InventoryCountItem;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import br.com.systemcommerce.inventorycount.mapper.InventoryCountMapper;
import br.com.systemcommerce.inventorycount.repository.InventoryCountAdjustmentRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountEntryRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountItemRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountSessionRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountStatusHistoryRepository;
import br.com.systemcommerce.catalog.repository.BrandRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.repository.StorageLocationRepository;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceTest {

    @Mock
    private InventoryCountRepository countRepository;
    @Mock
    private InventoryCountItemRepository itemRepository;
    @Mock
    private InventoryCountEntryRepository entryRepository;
    @Mock
    private InventoryCountAdjustmentRepository adjustmentRepository;
    @Mock
    private InventoryCountSessionRepository sessionRepository;
    @Mock
    private InventoryCountStatusHistoryRepository historyRepository;
    @Mock
    private InventoryCountMapper mapper;
    @Mock
    private StoreService storeService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private StorageLocationRepository storageLocationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InventoryAdjustmentReasonRepository adjustmentReasonRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DomainAuditService domainAuditService;

    private InventoryCountService inventoryCountService;

    private UUID countId;
    private UUID warehouseId;
    private UUID productId;
    private InventoryCount count;
    private InventoryCountItem item;
    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID().toString(), "n/a", List.of()));

        inventoryCountService = new InventoryCountService(
                countRepository,
                itemRepository,
                entryRepository,
                adjustmentRepository,
                sessionRepository,
                historyRepository,
                mapper,
                storeService,
                warehouseService,
                productRepository,
                categoryRepository,
                brandRepository,
                storageLocationRepository,
                inventoryRepository,
                inventoryService,
                adjustmentReasonRepository,
                movementRepository,
                storeAuthorizationEvaluator,
                userRepository,
                domainAuditService);

        countId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        productId = UUID.randomUUID();

        warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        Store store = new Store();
        store.setId(UUID.randomUUID());

        count = new InventoryCount();
        count.setId(countId);
        count.setStatus(InventoryCountStatus.APPROVED);
        count.setCountNumber("INV-001");
        count.setWarehouse(warehouse);
        count.setStore(store);

        product = new Product();
        product.setId(productId);
        product.setSku("SKU-1");

        item = new InventoryCountItem();
        item.setId(UUID.randomUUID());
        item.setInventoryCount(count);
        item.setProduct(product);
        item.setVarianceQuantity(new BigDecimal("5.000"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void post_shouldCallRegisterInventoryMovementForVariance() {
        InventoryAdjustmentReason reason = new InventoryAdjustmentReason();
        reason.setId(UUID.randomUUID());
        reason.setCode("INVENTORY_COUNT");
        reason.setDescription("Inventário");

        UUID movementId = UUID.randomUUID();
        InventoryMovement movement = new InventoryMovement();
        movement.setId(movementId);

        when(countRepository.findDetailedById(countId)).thenReturn(Optional.of(count));
        when(itemRepository.findActiveByInventoryCountId(countId)).thenReturn(List.of(item));
        when(adjustmentReasonRepository.findByCodeAndActiveTrue("INVENTORY_COUNT")).thenReturn(Optional.of(reason));
        when(inventoryService.registerInventoryMovement(
                        eq(productId),
                        eq(warehouseId),
                        eq(new BigDecimal("5.000")),
                        eq(true),
                        eq(countId),
                        eq(reason),
                        any()))
                .thenReturn(new InventoryMovementResponse(
                        movementId,
                        productId,
                        "SKU-1",
                        "Produto",
                        null,
                        null,
                        warehouseId,
                        null,
                        InventoryMovement.MovementType.INVENTORY,
                        new BigDecimal("5.000"),
                        null,
                        null,
                        "INVENTORY_COUNT",
                        countId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Instant.now()));
        when(movementRepository.findById(movementId)).thenReturn(Optional.of(movement));
        when(countRepository.findDetailedById(countId)).thenReturn(Optional.of(count));
        when(itemRepository.findActiveByInventoryCountId(countId)).thenReturn(List.of(item));

        inventoryCountService.post(countId, new InventoryCountActionRequest("Postagem teste"));

        verify(inventoryService).registerInventoryMovement(
                eq(productId),
                eq(warehouseId),
                eq(new BigDecimal("5.000")),
                eq(true),
                eq(countId),
                eq(reason),
                any());
        assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.POSTED);
    }
}
