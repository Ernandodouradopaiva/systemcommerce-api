package br.com.systemcommerce.inventory.service;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentReasonResponse;
import br.com.systemcommerce.inventory.dto.InventoryAdjustmentRequest;
import br.com.systemcommerce.inventory.dto.InventoryAvailabilityResponse;
import br.com.systemcommerce.inventory.dto.InventoryBalanceResponse;
import br.com.systemcommerce.inventory.dto.InventoryConsolidatedBalanceResponse;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.dto.InventoryExitRequest;
import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.entity.Inventory;
import br.com.systemcommerce.inventory.entity.InventoryAdjustmentReason;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.mapper.InventoryMapper;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.inventory.specification.InventorySpecifications;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Set<InventoryMovement.MovementType> ADJUSTMENT_TYPES = EnumSet.of(
            InventoryMovement.MovementType.ADJUSTMENT_POSITIVE,
            InventoryMovement.MovementType.ADJUSTMENT_NEGATIVE,
            InventoryMovement.MovementType.CORRECTION);

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryAdjustmentReasonRepository reasonRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseService warehouseService;
    private final InventoryMapper inventoryMapper;
    private final DomainAuditService domainAuditService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Transactional(readOnly = true)
    public Page<InventoryBalanceResponse> list(
            UUID productId, UUID warehouseId, String search, Boolean belowMinimum, Pageable pageable) {
        return list(productId, null, warehouseId, search, belowMinimum, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryBalanceResponse> list(
            UUID productId,
            UUID storeId,
            UUID warehouseId,
            String search,
            Boolean belowMinimum,
            Pageable pageable) {
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        } else if (warehouseId != null) {
            Warehouse warehouse = warehouseService.requireUsable(warehouseId);
            storeAuthorizationEvaluator.assertCanAccess(
                    CurrentUser.requireId(), warehouse.getStore().getId());
        }
        return inventoryRepository
                .findAll(
                        InventorySpecifications.withFilters(
                                productId, storeId, warehouseId, search, belowMinimum),
                        pageable)
                .map(inventoryMapper::toBalance);
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(UUID productId) {
        return getBalance(productId, null);
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(UUID productId, UUID warehouseId) {
        Warehouse warehouse = resolveWarehouse(warehouseId);
        storeAuthorizationEvaluator.assertCanAccess(
                CurrentUser.requireId(), warehouse.getStore().getId());
        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(productId, warehouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estoque do produto", productId));
        return inventoryMapper.toBalance(inventory);
    }

    /** Quantidade disponível no depósito (fórmulas oficiais); zero se ainda não houver saldo. */
    @Transactional(readOnly = true)
    public BigDecimal availableQuantity(UUID productId, UUID warehouseId) {
        Warehouse warehouse = resolveWarehouse(warehouseId);
        return inventoryRepository
                .findByProductIdAndWarehouseId(productId, warehouse.getId())
                .map(InventoryBalanceFormulas::available)
                .orElse(BigDecimal.ZERO)
                .setScale(3, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public InventoryConsolidatedBalanceResponse getConsolidatedBalance(UUID productId) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        List<Inventory> rows = inventoryRepository.findAllByProductId(productId);
        List<InventoryBalanceResponse> balances = rows.stream().map(inventoryMapper::toBalance).toList();
        BigDecimal physical = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal blocked = BigDecimal.ZERO;
        BigDecimal inTransit = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;
        for (Inventory inv : rows) {
            physical = physical.add(InventoryBalanceFormulas.physical(inv));
            reserved = reserved.add(InventoryBalanceFormulas.reserved(inv));
            blocked = blocked.add(InventoryBalanceFormulas.blocked(inv));
            inTransit = inTransit.add(InventoryBalanceFormulas.inTransit(inv));
            available = available.add(InventoryBalanceFormulas.available(inv));
        }
        return new InventoryConsolidatedBalanceResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                InventoryBalanceFormulas.scale(physical),
                InventoryBalanceFormulas.scale(reserved),
                InventoryBalanceFormulas.scale(blocked),
                InventoryBalanceFormulas.scale(inTransit),
                InventoryBalanceFormulas.scale(available),
                balances);
    }

    @Transactional(readOnly = true)
    public InventoryAvailabilityResponse checkAvailability(UUID productId, UUID warehouseId) {
        Warehouse warehouse = resolveWarehouse(warehouseId);
        return inventoryRepository
                .findByProductIdAndWarehouseId(productId, warehouse.getId())
                .map(inv -> inventoryMapper.toAvailability(inv, null))
                .orElseGet(() -> inventoryMapper.emptyAvailability(
                        productId,
                        warehouse.getStore() != null ? warehouse.getStore().getId() : null,
                        warehouse.getId(),
                        "Sem saldo cadastrado neste depósito"));
    }

    @Transactional
    public InventoryMovementResponse registerTransferOut(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID transferId) {
        return applyTransferMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.TRANSFER_OUT,
                quantity,
                false,
                false,
                transferId,
                "Saída por transferência");
    }

    @Transactional
    public InventoryMovementResponse registerTransferInTransit(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID transferId) {
        return applyTransferMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.TRANSFER_IN_TRANSIT,
                quantity,
                true,
                true,
                transferId,
                "Entrada em trânsito por transferência");
    }

    @Transactional
    public InventoryMovementResponse registerTransferReceive(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID transferId) {
        return applyTransferReceive(productId, warehouseId, quantity, transferId);
    }

    /** Reverte saída na origem ao cancelar transferência já despachada. */
    @Transactional
    public InventoryMovementResponse registerTransferRevertOut(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID transferId) {
        return applyTransferMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.TRANSFER_IN,
                quantity,
                true,
                false,
                transferId,
                "Estorno de saída por cancelamento de transferência");
    }

    /** Reverte quantidade em trânsito no destino ao cancelar transferência já despachada. */
    @Transactional
    public InventoryMovementResponse registerTransferRevertInTransit(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID transferId) {
        return applyTransferMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.TRANSFER_IN_TRANSIT,
                quantity,
                false,
                true,
                transferId,
                "Estorno de trânsito por cancelamento de transferência");
    }

    /**
     * Reserva formal de estoque (Prompt 70) — incrementa {@code quantityReserved} sob lock pessimista.
     * Não altera o saldo físico. Lança {@link BusinessRuleException} se a quantidade disponível for insuficiente.
     */
    @Transactional
    public void reserveQuantity(UUID productId, UUID warehouseId, BigDecimal rawQuantity) {
        try {
            doReserveQuantity(productId, warehouseId, rawQuantity);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência ao reservar estoque. Tente novamente.");
        }
    }

    private void doReserveQuantity(UUID productId, UUID warehouseId, BigDecimal rawQuantity) {
        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(rawQuantity);
        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Sem saldo de estoque neste depósito para reservar"));
        BigDecimal available = InventoryBalanceFormulas.available(inventory);
        if (available.compareTo(quantity) < 0) {
            throw new BusinessRuleException(
                    "Quantidade disponível insuficiente para reserva (disponível: " + available + ", solicitado: "
                            + quantity + ")");
        }
        BigDecimal previousReserved = InventoryBalanceFormulas.reserved(inventory);
        inventory.setQuantityReserved(previousReserved.add(quantity));
        inventoryRepository.saveAndFlush(inventory);
    }

    /**
     * Libera quantidade reservada (cancelamento/expiração/liberação parcial) — não altera o saldo físico.
     */
    @Transactional
    public void releaseReservedQuantity(UUID productId, UUID warehouseId, BigDecimal rawQuantity) {
        try {
            doAdjustReserved(productId, warehouseId, rawQuantity, "liberar");
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência ao liberar reserva. Tente novamente.");
        }
    }

    /**
     * Consome quantidade reservada (tipicamente no faturamento ou na conclusão da separação) — a baixa do saldo
     * físico é feita separadamente pelo faturamento (registerSale); este método apenas encerra a reserva.
     */
    @Transactional
    public void consumeReservedQuantity(UUID productId, UUID warehouseId, BigDecimal rawQuantity) {
        try {
            doAdjustReserved(productId, warehouseId, rawQuantity, "consumir");
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência ao consumir reserva. Tente novamente.");
        }
    }

    private void doAdjustReserved(UUID productId, UUID warehouseId, BigDecimal rawQuantity, String action) {
        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(rawQuantity);
        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new BusinessRuleException("Sem saldo de estoque neste depósito para " + action));
        BigDecimal previousReserved = InventoryBalanceFormulas.reserved(inventory);
        BigDecimal next = previousReserved.subtract(quantity);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Quantidade reservada insuficiente para " + action);
        }
        inventory.setQuantityReserved(next);
        inventoryRepository.saveAndFlush(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryBalanceResponse> listBelowMinimum(Pageable pageable) {
        return inventoryRepository.findBelowMinimum(pageable).map(inventoryMapper::toBalance);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> listMovements(
            UUID productId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            Instant from,
            Instant to,
            Pageable pageable) {
        return listMovements(productId, null, warehouseId, type, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> listMovements(
            UUID productId,
            UUID storeId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            Instant from,
            Instant to,
            Pageable pageable) {
        return movementRepository
                .findAll(
                        InventorySpecifications.movements(productId, storeId, warehouseId, type, from, to),
                        pageable)
                .map(inventoryMapper::toMovement);
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentReasonResponse> listActiveReasons() {
        return reasonRepository.findByActiveTrueOrderByDescriptionAsc().stream()
                .map(inventoryMapper::toReason)
                .toList();
    }

    @Transactional
    public InventoryMovementResponse registerEntry(InventoryEntryRequest request) {
        return registerEntry(request, "MANUAL", null);
    }

    @Transactional
    public InventoryMovementResponse registerEntry(InventoryEntryRequest request, String origin, UUID originId) {
        boolean futureReturn = Boolean.TRUE.equals(request.futureReturn());
        InventoryMovement.MovementType type =
                futureReturn ? InventoryMovement.MovementType.FUTURE_RETURN : InventoryMovement.MovementType.ENTRY;
        return applyMovement(
                request.productId(),
                request.warehouseId(),
                type,
                request.quantity(),
                true,
                origin,
                originId,
                null,
                futureReturn ? "Devolução futura" : "Entrada de estoque",
                request.observation());
    }

    /** Entrada oficial por nota de compra (stock entry confirmado). */
    @Transactional
    public InventoryMovementResponse registerStockEntry(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID stockEntryId, String observation) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.ENTRY,
                quantity,
                true,
                "STOCK_ENTRY",
                stockEntryId,
                null,
                "Entrada por nota de compra",
                observation);
    }

    /** Entrada oficial por recebimento de pedido de compra (Prompt 61). */
    @Transactional
    public InventoryMovementResponse registerPurchase(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID receiptId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.PURCHASE,
                quantity,
                true,
                "PURCHASE_RECEIPT",
                receiptId,
                null,
                "Entrada por recebimento de compra",
                null);
    }

    /** Baixa oficial de estoque por devolução ao fornecedor (Prompt 63) — nunca alterada diretamente. */
    @Transactional
    public InventoryMovementResponse registerSupplierReturn(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID supplierReturnId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.SUPPLIER_RETURN,
                quantity,
                false,
                "SUPPLIER_RETURN",
                supplierReturnId,
                null,
                "Saída por devolução ao fornecedor",
                null);
    }

    /** Ajuste por inventário físico postado (Prompt 74) — tipo INVENTORY. */
    @Transactional
    public InventoryMovementResponse registerInventoryMovement(
            UUID productId,
            UUID warehouseId,
            BigDecimal quantity,
            boolean increase,
            UUID inventoryCountId,
            InventoryAdjustmentReason reason,
            String observation) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.INVENTORY,
                quantity,
                increase,
                "INVENTORY_COUNT",
                inventoryCountId,
                reason,
                reason != null ? reason.getDescription() : "Inventário físico",
                observation);
    }

    /** Consumo de insumos na produção (Prompt 79). */
    @Transactional
    public InventoryMovementResponse registerProductionConsumption(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID productionOrderId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.PRODUCTION,
                quantity,
                false,
                "PRODUCTION_ORDER",
                productionOrderId,
                null,
                "Consumo na produção",
                null);
    }

    /** Entrada de produto acabado na produção (Prompt 79). */
    @Transactional
    public InventoryMovementResponse registerProductionOutput(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID productionOrderId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.PRODUCTION,
                quantity,
                true,
                "PRODUCTION_ORDER",
                productionOrderId,
                null,
                "Saída de produção (acabado)",
                null);
    }

    @Transactional
    public InventoryMovementResponse registerExit(InventoryExitRequest request) {
        return applyMovement(
                request.productId(),
                request.warehouseId(),
                InventoryMovement.MovementType.EXIT,
                request.quantity(),
                false,
                "MANUAL",
                null,
                null,
                "Saída de estoque",
                request.observation());
    }

    @Transactional
    public InventoryMovementResponse registerAdjustment(InventoryAdjustmentRequest request) {
        if (!ADJUSTMENT_TYPES.contains(request.type())) {
            throw new BusinessRuleException(
                    "Tipo de ajuste inválido. Use ADJUSTMENT_POSITIVE, ADJUSTMENT_NEGATIVE ou CORRECTION");
        }
        InventoryAdjustmentReason reason = reasonRepository
                .findByIdAndActiveTrue(request.reasonId())
                .orElseThrow(() -> new BusinessRuleException("Motivo de ajuste inválido ou inativo"));

        boolean increase = resolveAdjustmentIncrease(request);

        return applyMovement(
                request.productId(),
                request.warehouseId(),
                request.type(),
                request.quantity(),
                increase,
                "ADJUSTMENT",
                null,
                reason,
                reason.getDescription(),
                request.observation());
    }

    /** Baixa de estoque por venda no depósito padrão (backoffice / compatibilidade). */
    @Transactional
    public InventoryMovementResponse registerSale(UUID productId, BigDecimal quantity, UUID saleId) {
        return registerSale(productId, null, quantity, saleId);
    }

    @Transactional
    public InventoryMovementResponse registerSale(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID saleId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.SALE,
                quantity,
                false,
                "SALE",
                saleId,
                null,
                "Venda",
                null);
    }

    /** Reversão de venda: movimentação inversa no depósito padrão. */
    @Transactional
    public InventoryMovementResponse registerSaleCancel(UUID productId, BigDecimal quantity, UUID saleId) {
        return registerSaleCancel(productId, null, quantity, saleId);
    }

    @Transactional
    public InventoryMovementResponse registerSaleCancel(
            UUID productId, UUID warehouseId, BigDecimal quantity, UUID saleId) {
        return applyMovement(
                productId,
                warehouseId,
                InventoryMovement.MovementType.SALE_CANCEL,
                quantity,
                true,
                "SALE",
                saleId,
                null,
                "Cancelamento de venda",
                null);
    }

    private boolean resolveAdjustmentIncrease(InventoryAdjustmentRequest request) {
        return switch (request.type()) {
            case ADJUSTMENT_POSITIVE -> true;
            case ADJUSTMENT_NEGATIVE -> false;
            case CORRECTION -> {
                if (request.effect() == null) {
                    throw new BusinessRuleException("Correção exige o campo effect (INCREASE ou DECREASE)");
                }
                yield request.effect() == InventoryAdjustmentRequest.StockEffect.INCREASE;
            }
            default -> throw new BusinessRuleException("Tipo de ajuste inválido");
        };
    }

    private InventoryMovementResponse applyMovement(
            UUID productId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            BigDecimal rawQuantity,
            boolean increase,
            String origin,
            UUID originId,
            InventoryAdjustmentReason adjustmentReason,
            String reasonText,
            String observation) {
        try {
            return doApplyMovement(
                    productId,
                    warehouseId,
                    type,
                    rawQuantity,
                    increase,
                    origin,
                    originId,
                    adjustmentReason,
                    reasonText,
                    observation);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência no estoque. Tente novamente.");
        }
    }

    private InventoryMovementResponse doApplyMovement(
            UUID productId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            BigDecimal rawQuantity,
            boolean increase,
            String origin,
            UUID originId,
            InventoryAdjustmentReason adjustmentReason,
            String reasonText,
            String observation) {
        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(rawQuantity);
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        Warehouse warehouse = resolveWarehouseForMovement(warehouseId);

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouse.getId())
                .orElseGet(() -> createInventorySafe(product, warehouse));

        BigDecimal previous = inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
        BigDecimal next = increase ? previous.add(quantity) : previous.subtract(quantity);

        if (next.compareTo(BigDecimal.ZERO) < 0 && !Boolean.TRUE.equals(product.getAllowNegativeStock())) {
            throw new BusinessRuleException("Saída não pode deixar estoque negativo para este produto");
        }

        inventory.setQuantity(next);
        if (product.getMinStock() != null) {
            inventory.setMinimumQuantity(product.getMinStock());
        }
        inventoryRepository.saveAndFlush(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setStore(warehouse.getStore());
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setPreviousQuantity(previous);
        movement.setNewQuantity(next);
        movement.setOrigin(origin);
        movement.setOriginId(originId);
        movement.setReason(reasonText);
        movement.setAdjustmentReason(adjustmentReason);
        movement.setObservation(MoneyAndQuantityUtils.blankToNull(observation));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(movement::setUser);

        InventoryMovement saved = movementRepository.save(movement);
        saved.getProduct().getSku();
        saved.getWarehouse().getCode();
        if (saved.getUser() != null) {
            saved.getUser().getName();
        }
        if (saved.getAdjustmentReason() != null) {
            saved.getAdjustmentReason().getCode();
        }

        if (shouldAuditMovement(type, origin)) {
            domainAuditService.record(
                    "INVENTORY",
                    "InventoryMovement",
                    saved.getId(),
                    AuditLog.AuditAction.STOCK_MOVEMENT,
                    Map.of("quantity", previous),
                    movementSnapshot(saved),
                    reasonText != null ? reasonText : type.name());
        }

        return inventoryMapper.toMovement(saved);
    }

    private Warehouse resolveWarehouse(UUID warehouseId) {
        if (warehouseId == null) {
            return warehouseService.requireDefaultWarehouse();
        }
        return warehouseService.requireUsable(warehouseId);
    }

    /** Depósito bloqueado (Prompt 67) não pode receber novas movimentações de estoque. */
    private Warehouse resolveWarehouseForMovement(UUID warehouseId) {
        Warehouse warehouse = resolveWarehouse(warehouseId);
        if (!warehouse.isMovementAllowed()) {
            throw new BusinessRuleException(
                    "Depósito " + warehouse.getCode() + " está bloqueado para movimentação");
        }
        return warehouse;
    }

    private boolean shouldAuditMovement(InventoryMovement.MovementType type, String origin) {
        if ("STOCK_ENTRY".equals(origin) || "PURCHASE_RECEIPT".equals(origin)) {
            return true;
        }
        if ("ADJUSTMENT".equals(origin)) {
            return true;
        }
        return "MANUAL".equals(origin)
                && (type == InventoryMovement.MovementType.ENTRY
                        || type == InventoryMovement.MovementType.FUTURE_RETURN);
    }

    private Map<String, Object> movementSnapshot(InventoryMovement movement) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", movement.getId());
        map.put("productId", movement.getProduct().getId());
        map.put("productSku", movement.getProduct().getSku());
        map.put("warehouseId", movement.getWarehouse().getId());
        map.put("warehouseCode", movement.getWarehouse().getCode());
        map.put("type", movement.getType().name());
        map.put("quantity", movement.getQuantity());
        map.put("previousQuantity", movement.getPreviousQuantity());
        map.put("newQuantity", movement.getNewQuantity());
        map.put("origin", movement.getOrigin());
        map.put("originId", movement.getOriginId());
        map.put("reason", movement.getReason());
        map.put("observation", movement.getObservation());
        if (movement.getAdjustmentReason() != null) {
            map.put("adjustmentReasonCode", movement.getAdjustmentReason().getCode());
        }
        return map;
    }

    private Inventory createInventorySafe(Product product, Warehouse warehouse) {
        try {
            return createInventory(product, warehouse);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return inventoryRepository
                    .findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
                    .orElseThrow(() -> new BusinessRuleException("Falha ao obter saldo de estoque"));
        }
    }

    private Inventory createInventory(Product product, Warehouse warehouse) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setStore(warehouse.getStore());
        inventory.setQuantity(BigDecimal.ZERO);
        inventory.setQuantityReserved(BigDecimal.ZERO);
        inventory.setQuantityBlocked(BigDecimal.ZERO);
        inventory.setQuantityInTransit(BigDecimal.ZERO);
        BigDecimal min = product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO;
        inventory.setMinimumQuantity(min);
        inventory.setReorderPoint(min);
        return inventoryRepository.saveAndFlush(inventory);
    }

    private InventoryMovementResponse applyTransferReceive(
            UUID productId, UUID warehouseId, BigDecimal rawQuantity, UUID transferId) {
        try {
            return doApplyTransferReceive(productId, warehouseId, rawQuantity, transferId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência no estoque. Tente novamente.");
        }
    }

    private InventoryMovementResponse doApplyTransferReceive(
            UUID productId, UUID warehouseId, BigDecimal rawQuantity, UUID transferId) {
        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(rawQuantity);
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        Warehouse warehouse = resolveWarehouseForMovement(warehouseId);

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouse.getId())
                .orElseGet(() -> createInventorySafe(product, warehouse));

        BigDecimal previousPhysical =
                inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
        BigDecimal previousInTransit = inventory.getQuantityInTransit() != null
                ? inventory.getQuantityInTransit()
                : BigDecimal.ZERO;

        if (previousInTransit.compareTo(quantity) < 0) {
            throw new BusinessRuleException("Quantidade em trânsito insuficiente para recebimento");
        }

        BigDecimal nextInTransit = previousInTransit.subtract(quantity);
        BigDecimal nextPhysical = previousPhysical.add(quantity);

        inventory.setQuantityInTransit(nextInTransit);
        inventory.setQuantity(nextPhysical);
        if (product.getMinStock() != null) {
            inventory.setMinimumQuantity(product.getMinStock());
        }
        inventoryRepository.saveAndFlush(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setStore(warehouse.getStore());
        movement.setType(InventoryMovement.MovementType.TRANSFER_IN);
        movement.setQuantity(quantity);
        movement.setPreviousQuantity(previousPhysical);
        movement.setNewQuantity(nextPhysical);
        movement.setOrigin("STOCK_TRANSFER");
        movement.setOriginId(transferId);
        movement.setReason("Recebimento de transferência");
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(movement::setUser);

        InventoryMovement saved = movementRepository.save(movement);
        saved.getProduct().getSku();
        saved.getWarehouse().getCode();

        domainAuditService.record(
                "INVENTORY",
                "InventoryMovement",
                saved.getId(),
                AuditLog.AuditAction.STOCK_MOVEMENT,
                Map.of("quantity", previousPhysical, "quantityInTransit", previousInTransit),
                movementSnapshot(saved),
                "Recebimento de transferência");

        return inventoryMapper.toMovement(saved);
    }

    private InventoryMovementResponse applyTransferMovement(
            UUID productId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            BigDecimal rawQuantity,
            boolean increasePhysical,
            boolean inTransitField,
            UUID transferId,
            String reasonText) {
        try {
            return doApplyTransferMovement(
                    productId,
                    warehouseId,
                    type,
                    rawQuantity,
                    increasePhysical,
                    inTransitField,
                    transferId,
                    reasonText);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência no estoque. Tente novamente.");
        }
    }

    private InventoryMovementResponse doApplyTransferMovement(
            UUID productId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            BigDecimal rawQuantity,
            boolean increasePhysical,
            boolean inTransitField,
            UUID transferId,
            String reasonText) {
        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(rawQuantity);
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        Warehouse warehouse = resolveWarehouseForMovement(warehouseId);

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouse.getId())
                .orElseGet(() -> createInventorySafe(product, warehouse));

        BigDecimal previousPhysical =
                inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;
        BigDecimal previousInTransit = inventory.getQuantityInTransit() != null
                ? inventory.getQuantityInTransit()
                : BigDecimal.ZERO;

        BigDecimal nextPhysical = previousPhysical;
        BigDecimal nextInTransit = previousInTransit;

        if (inTransitField) {
            nextInTransit = increasePhysical ? previousInTransit.add(quantity) : previousInTransit.subtract(quantity);
            if (nextInTransit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Quantidade em trânsito não pode ficar negativa");
            }
            inventory.setQuantityInTransit(nextInTransit);
        } else {
            nextPhysical = increasePhysical ? previousPhysical.add(quantity) : previousPhysical.subtract(quantity);
            if (nextPhysical.compareTo(BigDecimal.ZERO) < 0
                    && !Boolean.TRUE.equals(product.getAllowNegativeStock())) {
                throw new BusinessRuleException("Saída não pode deixar estoque negativo para este produto");
            }
            inventory.setQuantity(nextPhysical);
        }

        if (product.getMinStock() != null) {
            inventory.setMinimumQuantity(product.getMinStock());
        }
        inventoryRepository.saveAndFlush(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setStore(warehouse.getStore());
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setPreviousQuantity(inTransitField ? previousInTransit : previousPhysical);
        movement.setNewQuantity(inTransitField ? nextInTransit : nextPhysical);
        movement.setOrigin("STOCK_TRANSFER");
        movement.setOriginId(transferId);
        movement.setReason(reasonText);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(movement::setUser);

        InventoryMovement saved = movementRepository.save(movement);
        saved.getProduct().getSku();
        saved.getWarehouse().getCode();

        domainAuditService.record(
                "INVENTORY",
                "InventoryMovement",
                saved.getId(),
                AuditLog.AuditAction.STOCK_MOVEMENT,
                Map.of("quantity", previousPhysical, "quantityInTransit", previousInTransit),
                movementSnapshot(saved),
                reasonText);

        return inventoryMapper.toMovement(saved);
    }
}
