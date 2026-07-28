package br.com.systemcommerce.production.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.production.dto.ProductionOrderActionRequest;
import br.com.systemcommerce.production.entity.BillOfMaterials;
import br.com.systemcommerce.production.entity.BillOfMaterialsItem;
import br.com.systemcommerce.production.entity.ProductionOrder;
import br.com.systemcommerce.production.entity.ProductionOrderStatus;
import br.com.systemcommerce.production.mapper.ProductionMapper;
import br.com.systemcommerce.production.repository.BillOfMaterialsItemRepository;
import br.com.systemcommerce.production.repository.BillOfMaterialsRepository;
import br.com.systemcommerce.production.repository.ProductionConsumptionRepository;
import br.com.systemcommerce.production.repository.ProductionOrderRepository;
import br.com.systemcommerce.production.repository.ProductionOrderStatusHistoryRepository;
import br.com.systemcommerce.production.repository.ProductionOutputRepository;
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
class ProductionOrderServiceTest {

    @Mock
    private ProductionOrderRepository orderRepository;
    @Mock
    private BillOfMaterialsRepository bomRepository;
    @Mock
    private BillOfMaterialsItemRepository bomItemRepository;
    @Mock
    private ProductionConsumptionRepository consumptionRepository;
    @Mock
    private ProductionOutputRepository outputRepository;
    @Mock
    private ProductionOrderStatusHistoryRepository historyRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private ProductionMapper mapper;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private StoreService storeService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    @Mock
    private UserRepository userRepository;

    private ProductionOrderService productionOrderService;

    private UUID orderId;
    private UUID warehouseId;
    private UUID componentProductId;
    private UUID finishedProductId;
    private ProductionOrder order;
    private BillOfMaterialsItem bomItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID().toString(), "n/a", List.of()));

        productionOrderService = new ProductionOrderService(
                orderRepository,
                bomRepository,
                bomItemRepository,
                consumptionRepository,
                outputRepository,
                historyRepository,
                movementRepository,
                mapper,
                organizationService,
                storeService,
                warehouseService,
                inventoryService,
                storeAuthorizationEvaluator,
                userRepository);

        orderId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        componentProductId = UUID.randomUUID();
        finishedProductId = UUID.randomUUID();

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        Store store = new Store();
        store.setId(UUID.randomUUID());

        Product finished = new Product();
        finished.setId(finishedProductId);
        finished.setSku("ACABADO");

        Product component = new Product();
        component.setId(componentProductId);
        component.setSku("INSUMO");
        component.setCostPrice(new BigDecimal("10.00"));

        BillOfMaterials bom = new BillOfMaterials();
        bom.setId(UUID.randomUUID());

        order = new ProductionOrder();
        order.setId(orderId);
        order.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order.setWarehouse(warehouse);
        order.setStore(store);
        order.setBillOfMaterials(bom);
        order.setFinishedProduct(finished);
        order.setQuantityPlanned(new BigDecimal("2.000"));

        bomItem = new BillOfMaterialsItem();
        bomItem.setComponentProduct(component);
        bomItem.setQuantity(new BigDecimal("1.000"));
        bomItem.setScrapPercent(BigDecimal.ZERO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void complete_shouldConsumeComponentsAndRegisterOutput() {
        UUID consumeMovementId = UUID.randomUUID();
        UUID outputMovementId = UUID.randomUUID();

        when(orderRepository.findDetailedById(orderId)).thenReturn(Optional.of(order));
        when(bomItemRepository.findActiveByBillOfMaterialsId(order.getBillOfMaterials().getId()))
                .thenReturn(List.of(bomItem));
        when(inventoryService.availableQuantity(componentProductId, warehouseId)).thenReturn(new BigDecimal("100"));
        when(inventoryService.registerProductionConsumption(
                        eq(componentProductId), eq(warehouseId), eq(new BigDecimal("2.000")), eq(orderId)))
                .thenReturn(movementResponse(consumeMovementId, componentProductId));
        when(inventoryService.registerProductionOutput(
                        eq(finishedProductId), eq(warehouseId), eq(new BigDecimal("2.000")), eq(orderId)))
                .thenReturn(movementResponse(outputMovementId, finishedProductId));
        when(movementRepository.findById(consumeMovementId))
                .thenReturn(Optional.of(movementEntity(consumeMovementId)));
        when(movementRepository.findById(outputMovementId))
                .thenReturn(Optional.of(movementEntity(outputMovementId)));
        when(orderRepository.findDetailedById(orderId)).thenReturn(Optional.of(order));

        productionOrderService.complete(orderId, new ProductionOrderActionRequest("Concluir"));

        verify(inventoryService).registerProductionConsumption(
                eq(componentProductId), eq(warehouseId), eq(new BigDecimal("2.000")), eq(orderId));
        verify(inventoryService).registerProductionOutput(
                eq(finishedProductId), eq(warehouseId), eq(new BigDecimal("2.000")), eq(orderId));
        assertThat(order.getStatus()).isEqualTo(ProductionOrderStatus.COMPLETED);
        assertThat(order.getQuantityCompleted()).isEqualByComparingTo("2.000");
    }

    private InventoryMovementResponse movementResponse(UUID id, UUID productId) {
        return new InventoryMovementResponse(
                id,
                productId,
                "SKU",
                "Produto",
                null,
                null,
                warehouseId,
                null,
                InventoryMovement.MovementType.PRODUCTION,
                BigDecimal.ONE,
                null,
                null,
                "PRODUCTION_ORDER",
                orderId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now());
    }

    private br.com.systemcommerce.inventory.entity.InventoryMovement movementEntity(UUID id) {
        br.com.systemcommerce.inventory.entity.InventoryMovement movement =
                new br.com.systemcommerce.inventory.entity.InventoryMovement();
        movement.setId(id);
        return movement;
    }
}
