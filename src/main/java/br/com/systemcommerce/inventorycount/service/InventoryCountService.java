package br.com.systemcommerce.inventorycount.service;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.repository.BrandRepository;
import br.com.systemcommerce.inventory.entity.InventoryAdjustmentReason;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.inventory.service.InventoryBalanceFormulas;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.inventorycount.dto.InventoryCountActionRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountCreateRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountEntryRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountItemCreateRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountResponse;
import br.com.systemcommerce.inventorycount.dto.InventoryCountStatusHistoryResponse;
import br.com.systemcommerce.inventorycount.entity.InventoryCount;
import br.com.systemcommerce.inventorycount.entity.InventoryCountAdjustment;
import br.com.systemcommerce.inventorycount.entity.InventoryCountEntry;
import br.com.systemcommerce.inventorycount.entity.InventoryCountItem;
import br.com.systemcommerce.inventorycount.entity.InventoryCountSession;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatusHistory;
import br.com.systemcommerce.inventorycount.mapper.InventoryCountMapper;
import br.com.systemcommerce.inventorycount.repository.InventoryCountAdjustmentRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountEntryRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountItemRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountSessionRepository;
import br.com.systemcommerce.inventorycount.repository.InventoryCountStatusHistoryRepository;
import br.com.systemcommerce.inventorycount.specification.InventoryCountSpecifications;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.repository.StorageLocationRepository;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InventoryCountService {

    private static final EnumSet<InventoryCountStatus> COUNTABLE = EnumSet.of(
            InventoryCountStatus.OPEN, InventoryCountStatus.COUNTING, InventoryCountStatus.RECOUNTING);

    private final InventoryCountRepository countRepository;
    private final InventoryCountItemRepository itemRepository;
    private final InventoryCountEntryRepository entryRepository;
    private final InventoryCountAdjustmentRepository adjustmentRepository;
    private final InventoryCountSessionRepository sessionRepository;
    private final InventoryCountStatusHistoryRepository historyRepository;
    private final InventoryCountMapper mapper;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final InventoryAdjustmentReasonRepository adjustmentReasonRepository;
    private final InventoryMovementRepository movementRepository;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<InventoryCountResponse> list(
            UUID storeId, UUID warehouseId, InventoryCountStatus status, String search, Pageable pageable) {
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return countRepository
                .findAll(InventoryCountSpecifications.withFilters(storeId, warehouseId, status, search), pageable)
                .map(count -> mapper.toResponse(count, itemRepository.findActiveByInventoryCountId(count.getId())));
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse getById(UUID id) {
        InventoryCount count = requireAccessible(id);
        return mapper.toResponse(count, itemRepository.findActiveByInventoryCountId(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryCountStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return historyRepository.findByInventoryCountIdOrderByChangedAtAsc(id).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public InventoryCountResponse create(InventoryCountCreateRequest request) {
        Store store = storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), request.storeId());
        Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }

        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = countRepository.findByOrganizationIdAndIdempotencyKey(
                    store.getOrganization().getId(), request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return getById(existing.get().getId());
            }
        }

        InventoryCount count = new InventoryCount();
        count.setOrganization(store.getOrganization());
        count.setStore(store);
        count.setWarehouse(warehouse);
        count.setCountNumber(nextCountNumber(store.getOrganization().getId()));
        count.setCountType(request.countType());
        count.setStatus(InventoryCountStatus.PLANNED);
        count.setFreezeBalances(Boolean.TRUE.equals(request.freezeBalances()));
        count.setHideTheoreticalQty(
                request.hideTheoreticalQty() != null ? request.hideTheoreticalQty() : Boolean.TRUE);
        count.setRequireSecondCount(Boolean.TRUE.equals(request.requireSecondCount()));
        count.setPlannedAt(request.plannedAt());
        count.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        if (StringUtils.hasText(request.idempotencyKey())) {
            count.setIdempotencyKey(request.idempotencyKey().trim());
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));
            count.setCategory(category);
        }
        if (request.brandId() != null) {
            Brand brand = brandRepository
                    .findById(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Marca", request.brandId()));
            count.setBrand(brand);
        }
        if (request.storageLocationId() != null) {
            StorageLocation location = storageLocationRepository
                    .findById(request.storageLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Localização", request.storageLocationId()));
            count.setStorageLocation(location);
        }

        InventoryCount saved = countRepository.save(count);
        if (request.items() != null) {
            int line = 1;
            for (InventoryCountItemCreateRequest itemReq : request.items()) {
                saved.addItem(buildItem(saved, warehouse, itemReq, line++));
            }
            countRepository.save(saved);
        }

        recordStatus(saved, null, InventoryCountStatus.PLANNED, "Inventário planejado");
        domainAuditService.record(
                "INVENTORY_COUNT",
                "InventoryCount",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Inventário criado");
        return getById(saved.getId());
    }

    @Transactional
    public InventoryCountResponse open(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        assertStatus(count, InventoryCountStatus.PLANNED, "Abertura");
        if (itemRepository.countByInventoryCountIdAndActiveTrue(id) == 0) {
            throw new BusinessRuleException("Inventário deve conter ao menos um item");
        }
        count.setOpenedAt(Instant.now());
        changeStatus(count, InventoryCountStatus.OPEN, notesOrDefault(request, "Inventário aberto"));
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse startCounting(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        assertStatus(count, InventoryCountStatus.OPEN, "Início da contagem");
        InventoryCountSession session = new InventoryCountSession();
        session.setInventoryCount(count);
        session.setSessionNumber(sessionRepository.countByInventoryCountId(id) + 1);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(session::setCounterUser);
        sessionRepository.save(session);
        changeStatus(count, InventoryCountStatus.COUNTING, notesOrDefault(request, "Contagem iniciada"));
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse recordEntry(UUID id, InventoryCountEntryRequest request) {
        InventoryCount count = requireAccessible(id);
        if (!COUNTABLE.contains(count.getStatus())) {
            throw new BusinessRuleException("Contagem não permitida no status " + count.getStatus());
        }
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = entryRepository.findByInventoryCountIdAndIdempotencyKey(id, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return getById(id);
            }
        }

        InventoryCountItem item = itemRepository
                .findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item do inventário", request.itemId()));
        if (!item.getInventoryCount().getId().equals(id)) {
            throw new BusinessRuleException("Item não pertence a este inventário");
        }

        BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(request.quantity());
        int pass = request.countPass();
        if (pass == 1) {
            item.setCountedQuantity1(qty);
        } else {
            if (!Boolean.TRUE.equals(count.getRequireSecondCount())) {
                throw new BusinessRuleException("Segunda contagem não exigida neste inventário");
            }
            item.setCountedQuantity2(qty);
        }
        recalculateItemVariance(item);
        itemRepository.save(item);

        InventoryCountEntry entry = new InventoryCountEntry();
        entry.setInventoryCount(count);
        entry.setInventoryCountItem(item);
        entry.setCountPass(pass);
        entry.setQuantity(qty);
        entry.setBarcode(MoneyAndQuantityUtils.blankToNull(request.barcode()));
        if (StringUtils.hasText(request.idempotencyKey())) {
            entry.setIdempotencyKey(request.idempotencyKey().trim());
        }
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(entry::setEnteredBy);
        entryRepository.save(entry);

        if (count.getStatus() == InventoryCountStatus.OPEN) {
            changeStatus(count, InventoryCountStatus.COUNTING, "Entrada registrada");
        }
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse submitForAnalysis(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        if (count.getStatus() != InventoryCountStatus.COUNTING
                && count.getStatus() != InventoryCountStatus.RECOUNTING) {
            throw new BusinessRuleException("Envio para análise não permitido no status atual");
        }
        List<InventoryCountItem> items = itemRepository.findActiveByInventoryCountId(id);
        for (InventoryCountItem item : items) {
            if (item.getFinalCountedQuantity() == null) {
                throw new BusinessRuleException("Todos os itens devem ter quantidade contada final");
            }
        }
        if (Boolean.TRUE.equals(count.getRequireSecondCount()) && hasFirstSecondMismatch(items)) {
            changeStatus(count, InventoryCountStatus.RECOUNTING, notesOrDefault(request, "Divergência entre contagens"));
        } else {
            changeStatus(count, InventoryCountStatus.UNDER_ANALYSIS, notesOrDefault(request, "Enviado para análise"));
        }
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse approve(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        assertStatus(count, InventoryCountStatus.UNDER_ANALYSIS, "Aprovação");
        changeStatus(count, InventoryCountStatus.APPROVED, notesOrDefault(request, "Inventário aprovado"));
        count.setClosedAt(Instant.now());
        countRepository.save(count);
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse post(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        assertStatus(count, InventoryCountStatus.APPROVED, "Postagem");

        InventoryAdjustmentReason reason = adjustmentReasonRepository
                .findByCodeAndActiveTrue("INVENTORY_COUNT")
                .orElseThrow(() -> new BusinessRuleException("Motivo INVENTORY_COUNT não configurado"));

        UUID warehouseId = count.getWarehouse().getId();
        List<InventoryCountItem> items = itemRepository.findActiveByInventoryCountId(id);
        for (InventoryCountItem item : items) {
            BigDecimal variance = defaultZero(item.getVarianceQuantity());
            if (variance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            boolean increase = variance.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal qty = variance.abs();
            InventoryMovement movement = movementRepository
                    .findById(inventoryService.registerInventoryMovement(
                                    item.getProduct().getId(),
                                    warehouseId,
                                    qty,
                                    increase,
                                    count.getId(),
                                    reason,
                                    "Postagem inventário " + count.getCountNumber())
                            .id())
                    .orElseThrow(() -> new ResourceNotFoundException("Movimento de estoque", item.getId()));

            InventoryCountAdjustment adjustment = new InventoryCountAdjustment();
            adjustment.setInventoryCount(count);
            adjustment.setInventoryCountItem(item);
            adjustment.setProduct(item.getProduct());
            adjustment.setVarianceQuantity(variance);
            adjustment.setInventoryMovement(movement);
            CurrentUser.id().flatMap(userRepository::findById).ifPresent(adjustment::setPostedBy);
            adjustmentRepository.save(adjustment);
        }

        count.setPostedAt(Instant.now());
        changeStatus(count, InventoryCountStatus.POSTED, notesOrDefault(request, "Inventário postado"));
        domainAuditService.record(
                "INVENTORY_COUNT",
                "InventoryCount",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                Map.of("status", InventoryCountStatus.APPROVED.name()),
                snapshot(count),
                "Inventário postado");
        return getById(id);
    }

    @Transactional
    public InventoryCountResponse cancel(UUID id, InventoryCountActionRequest request) {
        InventoryCount count = requireAccessible(id);
        if (count.getStatus().isTerminal()) {
            throw new BusinessRuleException("Inventário já encerrado");
        }
        changeStatus(count, InventoryCountStatus.CANCELLED, notesOrDefault(request, "Inventário cancelado"));
        return getById(id);
    }

    private InventoryCountItem buildItem(
            InventoryCount count, Warehouse warehouse, InventoryCountItemCreateRequest req, int line) {
        Product product = productRepository
                .findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", req.productId()));
        StorageLocation location = null;
        if (req.storageLocationId() != null) {
            location = storageLocationRepository
                    .findById(req.storageLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Localização", req.storageLocationId()));
        }
        BigDecimal theoretical = req.theoreticalQuantity();
        if (theoretical == null) {
            theoretical = inventoryRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .map(InventoryBalanceFormulas::physical)
                    .orElse(BigDecimal.ZERO);
        }
        InventoryCountItem item = new InventoryCountItem();
        item.setInventoryCount(count);
        item.setProduct(product);
        item.setStorageLocation(location);
        item.setLineNumber(line);
        item.setTheoreticalQuantity(scale(theoretical));
        item.setUnitCost(product.getCostPrice());
        item.setFrozen(Boolean.TRUE.equals(count.getFreezeBalances()));
        item.setNotes(MoneyAndQuantityUtils.blankToNull(req.notes()));
        return item;
    }

    private void recalculateItemVariance(InventoryCountItem item) {
        BigDecimal finalQty;
        if (item.getCountedQuantity2() != null) {
            finalQty = item.getCountedQuantity2();
        } else if (item.getCountedQuantity1() != null) {
            finalQty = item.getCountedQuantity1();
        } else {
            finalQty = null;
        }
        item.setFinalCountedQuantity(finalQty);
        if (finalQty != null) {
            item.setVarianceQuantity(scale(finalQty.subtract(defaultZero(item.getTheoreticalQuantity()))));
        }
    }

    private boolean hasFirstSecondMismatch(List<InventoryCountItem> items) {
        return items.stream()
                .anyMatch(i -> i.getCountedQuantity1() != null
                        && i.getCountedQuantity2() != null
                        && i.getCountedQuantity1().compareTo(i.getCountedQuantity2()) != 0);
    }

    private InventoryCount requireAccessible(UUID id) {
        InventoryCount count = countRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventário", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), count.getStore().getId());
        return count;
    }

    private void assertStatus(InventoryCount count, InventoryCountStatus expected, String action) {
        if (count.getStatus() != expected) {
            throw new BusinessRuleException(action + " não permitida no status " + count.getStatus());
        }
    }

    private void changeStatus(InventoryCount count, InventoryCountStatus target, String notes) {
        InventoryCountStatus from = count.getStatus();
        if (from == target) {
            return;
        }
        count.setStatus(target);
        countRepository.save(count);
        recordStatus(count, from, target, notes);
    }

    private void recordStatus(
            InventoryCount count, InventoryCountStatus from, InventoryCountStatus to, String notes) {
        InventoryCountStatusHistory history = new InventoryCountStatusHistory();
        history.setInventoryCount(count);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        historyRepository.save(history);
    }

    private String nextCountNumber(UUID organizationId) {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "INV-" + datePart + "-";
        long seq = countRepository.countByOrganizationIdAndCountNumberStartingWith(organizationId, prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private String notesOrDefault(InventoryCountActionRequest request, String defaultNotes) {
        return request != null && StringUtils.hasText(request.notes()) ? request.notes().trim() : defaultNotes;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, Object> snapshot(InventoryCount count) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", count.getId());
        map.put("countNumber", count.getCountNumber());
        map.put("status", count.getStatus().name());
        return map;
    }
}
