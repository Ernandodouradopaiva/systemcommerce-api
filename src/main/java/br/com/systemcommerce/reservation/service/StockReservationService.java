package br.com.systemcommerce.reservation.service;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.reservation.dto.StockReservationCreateRequest;
import br.com.systemcommerce.reservation.dto.StockReservationItemRequest;
import br.com.systemcommerce.reservation.dto.StockReservationLineRequest;
import br.com.systemcommerce.reservation.dto.StockReservationResponse;
import br.com.systemcommerce.reservation.dto.StockReservationStatusHistoryResponse;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.entity.StockReservationItem;
import br.com.systemcommerce.reservation.entity.StockReservationStatusHistory;
import br.com.systemcommerce.reservation.mapper.StockReservationMapper;
import br.com.systemcommerce.reservation.repository.StockReservationRepository;
import br.com.systemcommerce.reservation.repository.StockReservationStatusHistoryRepository;
import br.com.systemcommerce.reservation.specification.StockReservationSpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Reserva formal de estoque (Prompt 70). Nunca altera o saldo físico — apenas {@code quantityReserved}
 * do {@code Inventory}, via {@link InventoryService}, sempre sob lock pessimista.
 */
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private static final Set<StockReservation.ReservationStatus> OPEN_STATUSES =
            EnumSet.of(StockReservation.ReservationStatus.ACTIVE, StockReservation.ReservationStatus.PARTIALLY_CONSUMED);

    private final StockReservationRepository reservationRepository;
    private final StockReservationStatusHistoryRepository historyRepository;
    private final StockReservationMapper mapper;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<StockReservationResponse> list(
            StockReservation.ReservationStatus status,
            UUID storeId,
            StockReservation.OriginType originType,
            UUID originId,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return reservationRepository
                .findAll(
                        StockReservationSpecifications.withFilters(
                                status, storeId, originType, originId, allowedStoreIds),
                        pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StockReservationResponse getById(UUID id) {
        return mapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<StockReservationStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return historyRepository.findByStockReservationIdOrderByChangedAtAsc(id).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    /**
     * Cria reserva a partir de QUOTE/SALES_ORDER (ou outras origens). Idempotente por
     * {@code (organizationId, idempotencyKey)}: retorna a reserva existente em vez de duplicar.
     */
    @Transactional
    public StockReservationResponse create(StockReservationCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        String normalizedKey = StringUtils.hasText(request.idempotencyKey())
                ? request.idempotencyKey().trim()
                : null;
        if (normalizedKey != null) {
            Optional<StockReservation> existing = reservationRepository.findByOrganizationIdAndIdempotencyKey(
                    store.getOrganization().getId(), normalizedKey);
            if (existing.isPresent()) {
                return mapper.toResponse(requireAccessible(existing.get().getId()));
            }
        }

        Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessRuleException("Reserva deve conter ao menos um item");
        }

        StockReservation reservation = new StockReservation();
        reservation.setOrganization(store.getOrganization());
        reservation.setStore(store);
        reservation.setWarehouse(warehouse);
        reservation.setReservationNumber(nextReservationNumber(store));
        reservation.setOriginType(request.originType());
        reservation.setOriginId(request.originId());
        reservation.setOriginNumber(MoneyAndQuantityUtils.blankToNull(request.originNumber()));
        reservation.setStatus(StockReservation.ReservationStatus.ACTIVE);
        reservation.setExpiresAt(request.expiresAt());
        reservation.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        reservation.setIdempotencyKey(normalizedKey);

        int line = 1;
        for (StockReservationItemRequest itemRequest : request.items()) {
            Product product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", itemRequest.productId()));
            BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(itemRequest.quantity());

            StockReservationItem item = new StockReservationItem();
            item.setProduct(product);
            item.setLineNumber(line++);
            item.setQuantityReserved(quantity);
            reservation.addItem(item);
        }

        StockReservation saved = reservationRepository.save(reservation);

        // Reserva efetiva no Inventory (lock pessimista, valida disponível) — após persistir o cabeçalho/itens
        // para garantir rollback consistente da reserva lógica caso a reserva física falhe.
        for (StockReservationItem item : saved.getItems()) {
            inventoryService.reserveQuantity(item.getProduct().getId(), warehouse.getId(), item.getQuantityReserved());
        }

        appendHistory(saved, null, StockReservation.ReservationStatus.ACTIVE, "Reserva criada");
        domainAuditService.record(
                "INVENTORY",
                "StockReservation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Reserva de estoque criada (" + saved.getOriginType() + " " + saved.getOriginNumber() + ")");
        return mapper.toResponse(requireAccessible(saved.getId()));
    }

    /** Consumo definitivo (tipicamente no faturamento) — encerra a reserva na quantidade informada por item. */
    @Transactional
    public StockReservationResponse consume(UUID id, List<StockReservationLineRequest> lines) {
        StockReservation reservation = requireAccessible(id);
        applyLines(reservation, lines, true);
        return mapper.toResponse(requireAccessible(id));
    }

    /**
     * Consome quantidades por origem (ex.: picking/faturamento), sem exigir o ID da reserva. Não falha se não
     * houver reserva aberta para a origem — integração é opcional/best-effort.
     */
    @Transactional
    public Optional<StockReservationResponse> consumeForOrigin(
            StockReservation.OriginType originType, UUID originId, List<StockReservationLineRequest> lines) {
        return findOpenReservationForOrigin(originType, originId).map(reservation -> {
            applyLines(reservation, lines, true);
            return mapper.toResponse(requireAccessible(reservation.getId()));
        });
    }

    /** Libera quantidades por origem (ex.: divergência/quebra na separação). Best-effort, não falha se ausente. */
    @Transactional
    public Optional<StockReservationResponse> releaseForOrigin(
            StockReservation.OriginType originType, UUID originId, List<StockReservationLineRequest> lines) {
        return findOpenReservationForOrigin(originType, originId).map(reservation -> {
            applyLines(reservation, lines, false);
            return mapper.toResponse(requireAccessible(reservation.getId()));
        });
    }

    /** Libera integralmente o saldo restante (todos os itens) da reserva — usado por cancelamento manual. */
    @Transactional
    public StockReservationResponse release(UUID id, String notes) {
        StockReservation reservation = requireAccessible(id);
        return releaseRemaining(
                reservation, StockReservation.ReservationStatus.RELEASED, notes != null ? notes : "Reserva liberada");
    }

    @Transactional
    public StockReservationResponse cancel(UUID id, String notes) {
        StockReservation reservation = requireAccessible(id);
        if (!reservation.isOpen()) {
            return mapper.toResponse(reservation);
        }
        return releaseRemaining(
                reservation,
                StockReservation.ReservationStatus.CANCELLED,
                notes != null ? notes : "Reserva cancelada");
    }

    /** Job de expiração — libera reservas ACTIVE/PARTIALLY_CONSUMED com {@code expiresAt} vencido. */
    @Scheduled(cron = "${systemcommerce.reservation.expire-cron:0 */15 * * * *}")
    @Transactional
    public void expireActivePastDue() {
        expireExpired();
    }

    @Transactional
    public int expireExpired() {
        List<StockReservation> pastDue = reservationRepository.findActivePastDue(Instant.now());
        for (StockReservation reservation : pastDue) {
            releaseRemaining(reservation, StockReservation.ReservationStatus.EXPIRED, "Reserva expirada automaticamente");
        }
        return pastDue.size();
    }

    private StockReservationResponse releaseRemaining(
            StockReservation reservation, StockReservation.ReservationStatus finalStatus, String notes) {
        StockReservation.ReservationStatus from = reservation.getStatus();
        for (StockReservationItem item : reservation.getItems()) {
            BigDecimal remaining = item.remaining();
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                inventoryService.releaseReservedQuantity(
                        item.getProduct().getId(), reservation.getWarehouse().getId(), remaining);
                item.setQuantityReleased(item.getQuantityReleased().add(remaining));
            }
        }
        reservation.setStatus(finalStatus);
        reservationRepository.save(reservation);
        appendHistory(reservation, from, finalStatus, notes);
        domainAuditService.record(
                "INVENTORY",
                "StockReservation",
                reservation.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(reservation),
                notes);
        return mapper.toResponse(requireAccessible(reservation.getId()));
    }

    private void applyLines(StockReservation reservation, List<StockReservationLineRequest> lines, boolean consume) {
        if (!reservation.isOpen()) {
            throw new BusinessRuleException("Reserva não está aberta (status: " + reservation.getStatus() + ")");
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }
        StockReservation.ReservationStatus from = reservation.getStatus();
        for (StockReservationLineRequest line : lines) {
            StockReservationItem item = reservation.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(line.productId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Produto " + line.productId() + " não consta na reserva " + reservation.getReservationNumber()));
            BigDecimal remaining = item.remaining();
            BigDecimal requested = MoneyAndQuantityUtils.positiveQuantity(line.quantity());
            BigDecimal applied = requested.min(remaining);
            if (applied.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (consume) {
                inventoryService.consumeReservedQuantity(
                        item.getProduct().getId(), reservation.getWarehouse().getId(), applied);
                item.setQuantityConsumed(item.getQuantityConsumed().add(applied));
            } else {
                inventoryService.releaseReservedQuantity(
                        item.getProduct().getId(), reservation.getWarehouse().getId(), applied);
                item.setQuantityReleased(item.getQuantityReleased().add(applied));
            }
        }
        StockReservation.ReservationStatus recomputed = recomputeStatus(reservation);
        reservation.setStatus(recomputed);
        reservationRepository.save(reservation);
        if (recomputed != from) {
            appendHistory(
                    reservation,
                    from,
                    recomputed,
                    consume ? "Reserva consumida (parcial/total)" : "Reserva liberada (parcial/total)");
        }
        domainAuditService.record(
                "INVENTORY",
                "StockReservation",
                reservation.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(reservation),
                consume ? "Consumo de reserva" : "Liberação parcial de reserva");
    }

    private StockReservation.ReservationStatus recomputeStatus(StockReservation reservation) {
        BigDecimal totalRemaining = BigDecimal.ZERO;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        for (StockReservationItem item : reservation.getItems()) {
            totalRemaining = totalRemaining.add(item.remaining());
            totalConsumed = totalConsumed.add(
                    item.getQuantityConsumed() != null ? item.getQuantityConsumed() : BigDecimal.ZERO);
        }
        if (totalRemaining.compareTo(BigDecimal.ZERO) > 0) {
            return totalConsumed.compareTo(BigDecimal.ZERO) > 0
                    ? StockReservation.ReservationStatus.PARTIALLY_CONSUMED
                    : StockReservation.ReservationStatus.ACTIVE;
        }
        return totalConsumed.compareTo(BigDecimal.ZERO) > 0
                ? StockReservation.ReservationStatus.CONSUMED
                : StockReservation.ReservationStatus.RELEASED;
    }

    private Optional<StockReservation> findOpenReservationForOrigin(
            StockReservation.OriginType originType, UUID originId) {
        if (originType == null || originId == null) {
            return Optional.empty();
        }
        List<StockReservation> found =
                reservationRepository.findByOriginAndStatusIn(originType, originId, OPEN_STATUSES);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    private String nextReservationNumber(Store store) {
        String prefix = "RES-" + store.getCode() + "-";
        long sequence = reservationRepository.countByReservationNumberPrefix(prefix) + 1;
        return prefix + String.format("%06d", sequence);
    }

    private void appendHistory(
            StockReservation reservation,
            StockReservation.ReservationStatus from,
            StockReservation.ReservationStatus to,
            String notes) {
        StockReservationStatusHistory history = new StockReservationStatusHistory();
        history.setStockReservation(reservation);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().ifPresent(history::setChangedBy);
        historyRepository.save(history);
    }

    private StockReservation requireAccessible(UUID id) {
        StockReservation reservation = reservationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de estoque", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), reservation.getStore().getId());
        return reservation;
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

    private Map<String, Object> snapshot(StockReservation reservation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reservationNumber", reservation.getReservationNumber());
        map.put("status", reservation.getStatus());
        map.put("storeId", reservation.getStore() != null ? reservation.getStore().getId() : null);
        map.put("warehouseId", reservation.getWarehouse() != null ? reservation.getWarehouse().getId() : null);
        map.put("originType", reservation.getOriginType());
        map.put("originId", reservation.getOriginId());
        return map;
    }
}
