package br.com.systemcommerce.picking.service;

import br.com.systemcommerce.picking.dto.PickingDivergenceRequest;
import br.com.systemcommerce.picking.dto.PickingItemPickRequest;
import br.com.systemcommerce.picking.dto.PickingOrderCreateRequest;
import br.com.systemcommerce.picking.dto.PickingOrderPrintDataResponse;
import br.com.systemcommerce.picking.dto.PickingOrderResponse;
import br.com.systemcommerce.picking.entity.PickingAssignment;
import br.com.systemcommerce.picking.entity.PickingDivergence;
import br.com.systemcommerce.picking.entity.PickingEvent;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.picking.entity.PickingOrderItem;
import br.com.systemcommerce.picking.mapper.PickingOrderMapper;
import br.com.systemcommerce.picking.repository.PickingAssignmentRepository;
import br.com.systemcommerce.picking.repository.PickingDivergenceRepository;
import br.com.systemcommerce.picking.repository.PickingEventRepository;
import br.com.systemcommerce.picking.repository.PickingOrderItemRepository;
import br.com.systemcommerce.picking.repository.PickingOrderRepository;
import br.com.systemcommerce.picking.specification.PickingOrderSpecifications;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.reservation.dto.StockReservationLineRequest;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.repository.StockReservationRepository;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.entity.SalesOrderItem;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Separação/picking (Prompt 71). Nunca baixa estoque físico — apenas organiza a separação. Ao completar,
 * marca o pedido como PICKED e aplica a política simples de reserva: consome o que foi separado e libera
 * o que ficou em falta (divergência de falta).
 */
@Service
@RequiredArgsConstructor
public class PickingOrderService {

    private static final Set<PickingOrder.PickingOrderStatus> TERMINAL =
            EnumSet.of(PickingOrder.PickingOrderStatus.PICKED, PickingOrder.PickingOrderStatus.CANCELLED);

    private final PickingOrderRepository pickingOrderRepository;
    private final PickingOrderItemRepository pickingOrderItemRepository;
    private final PickingAssignmentRepository pickingAssignmentRepository;
    private final PickingEventRepository pickingEventRepository;
    private final PickingDivergenceRepository pickingDivergenceRepository;
    private final StorePickingOrderSequenceService sequenceService;
    private final PickingOrderMapper mapper;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderService salesOrderService;
    private final StockReservationRepository stockReservationRepository;
    private final StockReservationService stockReservationService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<PickingOrderResponse> list(
            PickingOrder.PickingOrderStatus status,
            UUID storeId,
            UUID salesOrderId,
            UUID assignedToUserId,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return pickingOrderRepository
                .findAll(
                        PickingOrderSpecifications.withFilters(
                                status, storeId, salesOrderId, assignedToUserId, allowedStoreIds),
                        pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PickingOrderResponse getById(UUID id) {
        return mapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public PickingOrderPrintDataResponse printData(UUID id) {
        return mapper.toPrintData(requireAccessible(id));
    }

    @Transactional
    public PickingOrderResponse createFromSalesOrder(PickingOrderCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        SalesOrder salesOrder = salesOrderRepository
                .findDetailedById(request.salesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda", request.salesOrderId()));
        storeAuthorizationEvaluator.assertCanAccess(userId, salesOrder.getStore().getId());

        boolean alreadyOpen = pickingOrderRepository.findBySalesOrderId(salesOrder.getId()).stream()
                .anyMatch(p -> !TERMINAL.contains(p.getStatus()) || p.getStatus() == PickingOrder.PickingOrderStatus.PICKED);
        if (alreadyOpen) {
            throw new ConflictException("Pedido já possui separação em andamento ou concluída");
        }
        if (salesOrder.getWarehouse() == null) {
            throw new BusinessRuleException("Pedido sem depósito não pode gerar separação");
        }
        if (salesOrder.getItems().isEmpty()) {
            throw new BusinessRuleException("Pedido sem itens não pode gerar separação");
        }

        // Reaproveita a transição de status já validada pelo SalesOrderService (APPROVED -> PICKING).
        salesOrderService.startPicking(salesOrder.getId());

        PickingOrder order = new PickingOrder();
        order.setOrganization(salesOrder.getOrganization());
        order.setStore(salesOrder.getStore());
        order.setWarehouse(salesOrder.getWarehouse());
        order.setSalesOrder(salesOrder);
        order.setPickingNumber(sequenceService.allocateNextPickingNumber(salesOrder.getStore()));
        order.setStatus(PickingOrder.PickingOrderStatus.PENDING);
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        findOpenReservationForSalesOrder(salesOrder.getId()).ifPresent(order::setStockReservation);

        int line = 1;
        for (SalesOrderItem soItem : salesOrder.getItems()) {
            PickingOrderItem item = new PickingOrderItem();
            item.setSalesOrderItem(soItem);
            item.setProduct(soItem.getProduct());
            item.setLineNumber(line++);
            item.setQuantityRequested(soItem.getQuantity());
            item.setStorageLocationId(pickingOrderItemRepository.findPreferredStorageLocationId(
                    soItem.getProduct().getId(), salesOrder.getWarehouse().getId()));
            order.addItem(item);
        }

        PickingOrder saved = pickingOrderRepository.save(order);
        domainAuditService.record(
                "SALES",
                "PickingOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Separação criada a partir do pedido " + salesOrder.getOrderNumber());
        return mapper.toResponse(requireAccessible(saved.getId()));
    }

    @Transactional
    public PickingOrderResponse assign(UUID id, UUID userId) {
        PickingOrder order = requireAccessible(id);
        assertOpen(order);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        order.setAssignedTo(user);
        if (order.getStatus() == PickingOrder.PickingOrderStatus.PENDING) {
            order.setStatus(PickingOrder.PickingOrderStatus.ASSIGNED);
        }
        pickingOrderRepository.save(order);

        PickingAssignment assignment = new PickingAssignment();
        assignment.setPickingOrder(order);
        assignment.setUser(user);
        pickingAssignmentRepository.save(assignment);

        recordEvent(order, null, PickingEvent.PickingEventType.ASSIGNED, null, null, "Separação atribuída a " + user.getName(), null);
        return mapper.toResponse(requireAccessible(id));
    }

    @Transactional
    public PickingOrderResponse start(UUID id) {
        PickingOrder order = requireAccessible(id);
        assertOpen(order);
        if (order.getStartedAt() == null) {
            order.setStartedAt(Instant.now());
        }
        order.setStatus(PickingOrder.PickingOrderStatus.IN_PROGRESS);
        pickingOrderRepository.save(order);
        recordEvent(order, null, PickingEvent.PickingEventType.STARTED, null, null, "Separação iniciada", null);
        return mapper.toResponse(requireAccessible(id));
    }

    /** Bipagem de item por código de barras + quantidade — idempotente por {@code idempotencyKey}. */
    @Transactional
    public PickingOrderResponse pickItem(UUID id, PickingItemPickRequest request) {
        PickingOrder order = requireAccessible(id);
        assertOpen(order);

        String normalizedKey = StringUtils.hasText(request.idempotencyKey()) ? request.idempotencyKey().trim() : null;
        if (normalizedKey != null
                && pickingEventRepository.findByPickingOrderIdAndIdempotencyKey(id, normalizedKey).isPresent()) {
            return mapper.toResponse(requireAccessible(id));
        }

        String barcode = MoneyAndQuantityUtils.requireText(request.barcode(), "Código de barras");
        PickingOrderItem item = order.getItems().stream()
                .filter(i -> barcode.equals(i.getProduct().getBarcode()) && i.pending().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Nenhum item pendente encontrado para o código de barras informado"));

        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(request.quantity());
        if (quantity.compareTo(item.pending()) > 0) {
            throw new BusinessRuleException(
                    "Quantidade bipada excede o pendente do item (pendente: " + item.pending() + ")");
        }

        item.setQuantityPicked(item.getQuantityPicked().add(quantity));
        item.setBarcodeScanned(barcode);
        pickingOrderItemRepository.save(item);

        if (order.getStatus() == PickingOrder.PickingOrderStatus.PENDING
                || order.getStatus() == PickingOrder.PickingOrderStatus.ASSIGNED) {
            order.setStartedAt(order.getStartedAt() != null ? order.getStartedAt() : Instant.now());
        }
        recomputeProgressStatus(order);
        pickingOrderRepository.save(order);

        recordEvent(order, item, PickingEvent.PickingEventType.ITEM_PICKED, quantity, barcode, "Item bipado", normalizedKey);
        return mapper.toResponse(requireAccessible(id));
    }

    @Transactional
    public PickingOrderResponse recordDivergence(UUID id, PickingDivergenceRequest request) {
        PickingOrder order = requireAccessible(id);
        assertOpen(order);
        PickingOrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(request.itemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item da separação", request.itemId()));

        PickingDivergence divergence = new PickingDivergence();
        divergence.setPickingOrder(order);
        divergence.setPickingItem(item);
        divergence.setDivergenceType(request.divergenceType());
        divergence.setDescription(MoneyAndQuantityUtils.requireText(request.description(), "Descrição"));
        divergence.setQuantity(request.quantity());
        CurrentUser.id().ifPresent(divergence::setCreatedBy);
        pickingDivergenceRepository.save(divergence);

        order.setStatus(PickingOrder.PickingOrderStatus.DIVERGENT);
        pickingOrderRepository.save(order);

        recordEvent(
                order,
                item,
                PickingEvent.PickingEventType.DIVERGENCE,
                request.quantity(),
                null,
                request.description(),
                null);
        return mapper.toResponse(requireAccessible(id));
    }

    /**
     * Conclui a separação: marca o pedido como PICKED (via {@code SalesOrderService.markPicked}) e aplica a
     * política simples sobre a reserva vinculada (se houver): consome o que foi separado e libera a falta.
     */
    @Transactional
    public PickingOrderResponse complete(UUID id) {
        PickingOrder order = requireAccessible(id);
        assertOpen(order);

        boolean anyPicked = order.getItems().stream()
                .anyMatch(i -> i.getQuantityPicked() != null && i.getQuantityPicked().compareTo(BigDecimal.ZERO) > 0);
        if (!anyPicked) {
            throw new BusinessRuleException("Nenhum item foi separado; bipe os itens antes de concluir");
        }

        List<StockReservationLineRequest> consumeLines = new ArrayList<>();
        List<StockReservationLineRequest> releaseLines = new ArrayList<>();
        for (PickingOrderItem item : order.getItems()) {
            BigDecimal picked = item.getQuantityPicked() != null ? item.getQuantityPicked() : BigDecimal.ZERO;
            BigDecimal shortfall = item.pending();
            if (picked.compareTo(BigDecimal.ZERO) > 0) {
                consumeLines.add(new StockReservationLineRequest(item.getProduct().getId(), picked));
            }
            if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                releaseLines.add(new StockReservationLineRequest(item.getProduct().getId(), shortfall));
            }
        }

        if (order.getStockReservation() != null) {
            StockReservation.OriginType originType = StockReservation.OriginType.SALES_ORDER;
            UUID originId = order.getSalesOrder().getId();
            if (!consumeLines.isEmpty()) {
                stockReservationService.consumeForOrigin(originType, originId, consumeLines);
            }
            if (!releaseLines.isEmpty()) {
                stockReservationService.releaseForOrigin(originType, originId, releaseLines);
            }
        }

        order.setStatus(PickingOrder.PickingOrderStatus.PICKED);
        order.setCompletedAt(Instant.now());
        pickingOrderRepository.save(order);

        salesOrderService.markPicked(order.getSalesOrder().getId());

        recordEvent(order, null, PickingEvent.PickingEventType.COMPLETED, null, null, "Separação concluída", null);
        domainAuditService.record(
                "SALES",
                "PickingOrder",
                order.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(order),
                "Separação concluída — pedido marcado como PICKED");
        return mapper.toResponse(requireAccessible(id));
    }

    @Transactional
    public PickingOrderResponse cancel(UUID id, String notes) {
        PickingOrder order = requireAccessible(id);
        if (order.getStatus() == PickingOrder.PickingOrderStatus.CANCELLED) {
            return mapper.toResponse(order);
        }
        if (order.getStatus() == PickingOrder.PickingOrderStatus.PICKED) {
            throw new BusinessRuleException("Separação concluída não pode ser cancelada");
        }
        order.setStatus(PickingOrder.PickingOrderStatus.CANCELLED);
        pickingOrderRepository.save(order);
        recordEvent(
                order,
                null,
                PickingEvent.PickingEventType.CANCELLED,
                null,
                null,
                notes != null ? notes : "Separação cancelada",
                null);
        domainAuditService.record(
                "SALES",
                "PickingOrder",
                order.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(order),
                notes != null ? notes : "Separação cancelada");
        return mapper.toResponse(requireAccessible(id));
    }

    private void recomputeProgressStatus(PickingOrder order) {
        boolean anyPicked = false;
        boolean anyPending = false;
        for (PickingOrderItem item : order.getItems()) {
            BigDecimal picked = item.getQuantityPicked() != null ? item.getQuantityPicked() : BigDecimal.ZERO;
            if (picked.compareTo(BigDecimal.ZERO) > 0) {
                anyPicked = true;
            }
            if (item.pending().compareTo(BigDecimal.ZERO) > 0) {
                anyPending = true;
            }
        }
        if (anyPicked && anyPending) {
            order.setStatus(PickingOrder.PickingOrderStatus.PARTIALLY_PICKED);
        } else if (!anyPicked) {
            order.setStatus(PickingOrder.PickingOrderStatus.IN_PROGRESS);
        }
        // Totalmente separado (anyPicked && !anyPending): mantém status atual até conclusão explícita.
    }

    private java.util.Optional<StockReservation> findOpenReservationForSalesOrder(UUID salesOrderId) {
        List<StockReservation> found = stockReservationRepository.findByOriginAndStatusIn(
                StockReservation.OriginType.SALES_ORDER,
                salesOrderId,
                EnumSet.of(StockReservation.ReservationStatus.ACTIVE, StockReservation.ReservationStatus.PARTIALLY_CONSUMED));
        return found.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(found.get(0));
    }

    private void recordEvent(
            PickingOrder order,
            PickingOrderItem item,
            PickingEvent.PickingEventType type,
            BigDecimal quantity,
            String barcode,
            String notes,
            String idempotencyKey) {
        PickingEvent event = new PickingEvent();
        event.setPickingOrder(order);
        event.setPickingItem(item);
        event.setEventType(type);
        event.setQuantity(quantity);
        event.setBarcode(barcode);
        event.setNotes(notes);
        event.setIdempotencyKey(idempotencyKey);
        CurrentUser.id().ifPresent(event::setPerformedBy);
        pickingEventRepository.save(event);
    }

    private void assertOpen(PickingOrder order) {
        if (TERMINAL.contains(order.getStatus())) {
            throw new BusinessRuleException("Separação encerrada (status: " + order.getStatus() + ")");
        }
    }

    private PickingOrder requireAccessible(UUID id) {
        PickingOrder order = pickingOrderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Separação", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), order.getStore().getId());
        return order;
    }

    private Collection<UUID> resolveListStoreFilter(UUID storeId) {
        if (storeId != null) {
            return null;
        }
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return null;
        }
        if (SecurityAuthorities.hasAuthority("STORE_CONSOLIDATED_READ")) {
            return null;
        }
        return storeAuthorizationEvaluator.listEffectiveAccess(CurrentUser.requireId()).stream()
                .map(a -> a.getStore().getId())
                .toList();
    }

    private Map<String, Object> snapshot(PickingOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pickingNumber", order.getPickingNumber());
        map.put("status", order.getStatus());
        map.put("salesOrderId", order.getSalesOrder() != null ? order.getSalesOrder().getId() : null);
        map.put("storeId", order.getStore() != null ? order.getStore().getId() : null);
        map.put("warehouseId", order.getWarehouse() != null ? order.getWarehouse().getId() : null);
        return map;
    }
}
