package br.com.systemcommerce.production.service;

import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.production.dto.ProductionOrderActionRequest;
import br.com.systemcommerce.production.dto.ProductionOrderCreateRequest;
import br.com.systemcommerce.production.dto.ProductionOrderResponse;
import br.com.systemcommerce.production.dto.ProductionOrderStatusHistoryResponse;
import br.com.systemcommerce.production.entity.BillOfMaterials;
import br.com.systemcommerce.production.entity.BillOfMaterialsItem;
import br.com.systemcommerce.production.entity.ProductionConsumption;
import br.com.systemcommerce.production.entity.ProductionOrder;
import br.com.systemcommerce.production.entity.ProductionOrderStatus;
import br.com.systemcommerce.production.entity.ProductionOrderStatusHistory;
import br.com.systemcommerce.production.entity.ProductionOutput;
import br.com.systemcommerce.production.mapper.ProductionMapper;
import br.com.systemcommerce.production.repository.BillOfMaterialsItemRepository;
import br.com.systemcommerce.production.repository.BillOfMaterialsRepository;
import br.com.systemcommerce.production.repository.ProductionConsumptionRepository;
import br.com.systemcommerce.production.repository.ProductionOrderRepository;
import br.com.systemcommerce.production.repository.ProductionOrderStatusHistoryRepository;
import br.com.systemcommerce.production.repository.ProductionOutputRepository;
import br.com.systemcommerce.production.specification.ProductionOrderSpecifications;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductionOrderService {

    private final ProductionOrderRepository orderRepository;
    private final BillOfMaterialsRepository bomRepository;
    private final BillOfMaterialsItemRepository bomItemRepository;
    private final ProductionConsumptionRepository consumptionRepository;
    private final ProductionOutputRepository outputRepository;
    private final ProductionOrderStatusHistoryRepository historyRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductionMapper mapper;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ProductionOrderResponse> list(
            UUID storeId, UUID warehouseId, ProductionOrderStatus status, String search, Pageable pageable) {
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return orderRepository
                .findAll(ProductionOrderSpecifications.withFilters(storeId, warehouseId, status, search), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductionOrderResponse getById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderStatusHistoryResponse> statusHistory(UUID id) {
        getEntity(id);
        return historyRepository.findByProductionOrderIdOrderByChangedAtAsc(id).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public ProductionOrderResponse create(ProductionOrderCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = orderRepository.findByOrganizationIdAndIdempotencyKey(
                    organization.getId(), request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return getById(existing.get().getId());
            }
        }

        Store store = storeService.requireUsable(request.storeId());
        Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
        BillOfMaterials bom = bomRepository
                .findDetailedById(request.billOfMaterialsId())
                .orElseThrow(() -> new ResourceNotFoundException("Ficha técnica", request.billOfMaterialsId()));

        ProductionOrder order = new ProductionOrder();
        order.setOrganization(organization);
        order.setStore(store);
        order.setWarehouse(warehouse);
        order.setBillOfMaterials(bom);
        order.setFinishedProduct(bom.getFinishedProduct());
        order.setOrderNumber(nextOrderNumber());
        order.setStatus(ProductionOrderStatus.DRAFT);
        order.setQuantityPlanned(MoneyAndQuantityUtils.positiveQuantity(request.quantityPlanned()));
        order.setPlannedStart(request.plannedStart());
        order.setPlannedEnd(request.plannedEnd());
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        if (StringUtils.hasText(request.idempotencyKey())) {
            order.setIdempotencyKey(request.idempotencyKey().trim());
        }

        ProductionOrder saved = orderRepository.save(order);
        recordStatusChange(saved, null, ProductionOrderStatus.DRAFT, "Ordem criada");
        return getById(saved.getId());
    }

    @Transactional
    public ProductionOrderResponse plan(UUID id, ProductionOrderActionRequest request) {
        ProductionOrder order = getEntity(id);
        assertStatus(order, ProductionOrderStatus.DRAFT, "Planejamento");
        changeStatus(order, ProductionOrderStatus.PLANNED, notesOrDefault(request, "Ordem planejada"));
        return getById(id);
    }

    @Transactional
    public ProductionOrderResponse release(UUID id, ProductionOrderActionRequest request) {
        ProductionOrder order = getEntity(id);
        assertStatus(order, ProductionOrderStatus.PLANNED, "Liberação");
        changeStatus(order, ProductionOrderStatus.RELEASED, notesOrDefault(request, "Ordem liberada"));
        return getById(id);
    }

    @Transactional
    public ProductionOrderResponse start(UUID id, ProductionOrderActionRequest request) {
        ProductionOrder order = getEntity(id);
        assertStatus(order, ProductionOrderStatus.RELEASED, "Início");
        order.setStartedAt(Instant.now());
        changeStatus(order, ProductionOrderStatus.IN_PROGRESS, notesOrDefault(request, "Produção iniciada"));
        return getById(id);
    }

    @Transactional
    public ProductionOrderResponse complete(UUID id, ProductionOrderActionRequest request) {
        ProductionOrder order = getEntity(id);
        assertStatus(order, ProductionOrderStatus.IN_PROGRESS, "Conclusão");

        UUID warehouseId = order.getWarehouse().getId();
        List<BillOfMaterialsItem> bomItems = bomItemRepository.findActiveByBillOfMaterialsId(
                order.getBillOfMaterials().getId());
        BigDecimal plannedQty = order.getQuantityPlanned();
        BigDecimal totalCost = BigDecimal.ZERO;

        for (BillOfMaterialsItem bomItem : bomItems) {
            BigDecimal scrapFactor = BigDecimal.ONE.add(defaultZero(bomItem.getScrapPercent())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            BigDecimal consumeQty = bomItem.getQuantity().multiply(plannedQty).multiply(scrapFactor)
                    .setScale(3, RoundingMode.HALF_UP);
            Product component = bomItem.getComponentProduct();
            BigDecimal available = inventoryService.availableQuantity(component.getId(), warehouseId);
            if (available.compareTo(consumeQty) < 0) {
                throw new BusinessRuleException("Estoque insuficiente do componente " + component.getSku());
            }

            var movementResponse = inventoryService.registerProductionConsumption(
                    component.getId(), warehouseId, consumeQty, order.getId());
            InventoryMovement movement = movementRepository
                    .findById(movementResponse.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Movimento", movementResponse.id()));

            BigDecimal unitCost = defaultZero(component.getCostPrice());
            totalCost = totalCost.add(unitCost.multiply(consumeQty));

            ProductionConsumption consumption = new ProductionConsumption();
            consumption.setProductionOrder(order);
            consumption.setComponentProduct(component);
            consumption.setQuantity(consumeQty);
            consumption.setInventoryMovement(movement);
            consumption.setUnitCost(unitCost);
            consumptionRepository.save(consumption);
        }

        var outputMovementResponse = inventoryService.registerProductionOutput(
                order.getFinishedProduct().getId(), warehouseId, plannedQty, order.getId());
        InventoryMovement outputMovement = movementRepository
                .findById(outputMovementResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("Movimento", outputMovementResponse.id()));

        BigDecimal unitCost = plannedQty.compareTo(BigDecimal.ZERO) > 0
                ? totalCost.divide(plannedQty, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ProductionOutput output = new ProductionOutput();
        output.setProductionOrder(order);
        output.setProduct(order.getFinishedProduct());
        output.setQuantity(plannedQty);
        output.setInventoryMovement(outputMovement);
        output.setUnitCost(unitCost);
        outputRepository.save(output);

        order.setQuantityCompleted(plannedQty);
        order.setUnitCost(unitCost);
        order.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        order.setCompletedAt(Instant.now());
        changeStatus(order, ProductionOrderStatus.COMPLETED, notesOrDefault(request, "Produção concluída"));
        return getById(id);
    }

    private void changeStatus(ProductionOrder order, ProductionOrderStatus target, String notes) {
        ProductionOrderStatus from = order.getStatus();
        order.setStatus(target);
        orderRepository.save(order);
        recordStatusChange(order, from, target, notes);
    }

    private void recordStatusChange(
            ProductionOrder order, ProductionOrderStatus from, ProductionOrderStatus to, String notes) {
        User changedBy = CurrentUser.id().flatMap(userRepository::findById).orElse(null);
        ProductionOrderStatusHistory history = new ProductionOrderStatusHistory();
        history.setProductionOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    private void assertStatus(ProductionOrder order, ProductionOrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw new BusinessRuleException(action + " não permitida no status " + order.getStatus().name());
        }
    }

    private String notesOrDefault(ProductionOrderActionRequest request, String defaultNotes) {
        return request != null && StringUtils.hasText(request.notes()) ? request.notes().trim() : defaultNotes;
    }

    private String nextOrderNumber() {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "OP-" + datePart + "-";
        long sequence = orderRepository.countByOrderNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ProductionOrder getEntity(UUID id) {
        ProductionOrder order = orderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de produção", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), order.getStore().getId());
        return order;
    }
}
