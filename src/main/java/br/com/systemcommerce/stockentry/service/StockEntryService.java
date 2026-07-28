package br.com.systemcommerce.stockentry.service;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.stockentry.dto.StockEntryCreateRequest;
import br.com.systemcommerce.stockentry.dto.StockEntryItemCreateRequest;
import br.com.systemcommerce.stockentry.dto.StockEntryResponse;
import br.com.systemcommerce.stockentry.entity.StockEntry;
import br.com.systemcommerce.stockentry.entity.StockEntryItem;
import br.com.systemcommerce.stockentry.entity.StockEntryStatus;
import br.com.systemcommerce.stockentry.mapper.StockEntryMapper;
import br.com.systemcommerce.stockentry.repository.StockEntryItemRepository;
import br.com.systemcommerce.stockentry.repository.StockEntryRepository;
import br.com.systemcommerce.stockentry.specification.StockEntrySpecifications;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockEntryService {

    private final StockEntryRepository entryRepository;
    private final StockEntryItemRepository itemRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final StockEntryMapper stockEntryMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<StockEntryResponse> list(
            UUID organizationId, UUID storeId, UUID warehouseId, StockEntryStatus status, String search, Pageable pageable) {
        return entryRepository
                .findAll(
                        StockEntrySpecifications.withFilters(organizationId, storeId, warehouseId, status, search),
                        pageable)
                .map(stockEntryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StockEntryResponse getById(UUID id) {
        return stockEntryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StockEntryResponse create(StockEntryCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        Store store = storeService.requireUsable(request.storeId());
        Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
        assertSameOrganization(organization, store, warehouse);
        assertWarehouseBelongsToStore(warehouse, store);

        StockEntry entry = new StockEntry();
        entry.setOrganization(organization);
        entry.setStore(store);
        entry.setWarehouse(warehouse);
        entry.setNumber(nextEntryNumber(organization.getId()));
        entry.setSupplierName(MoneyAndQuantityUtils.blankToNull(request.supplierName()));
        entry.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(request.documentNumber()));
        entry.setEntryDate(request.entryDate());
        entry.setStatus(StockEntryStatus.DRAFT);
        entry.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(entry::setResponsibleUser);

        StockEntry saved = entryRepository.save(entry);
        audit(saved, AuditLog.AuditAction.CREATE, null, "Entrada de estoque criada");
        return stockEntryMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public StockEntryResponse addItem(UUID entryId, StockEntryItemCreateRequest request) {
        StockEntry entry = getEntity(entryId);
        assertStatus(entry, StockEntryStatus.DRAFT, "Inclusão de item");

        if (itemRepository
                .findByEntryIdAndProductIdAndActiveTrue(entryId, request.productId())
                .isPresent()) {
            throw new ConflictException("Produto já consta na entrada");
        }

        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessRuleException("Produto inativo não pode ser incluído na entrada");
        }

        BigDecimal quantity = MoneyAndQuantityUtils.positiveQuantity(request.quantity());
        BigDecimal unitCost = request.unitCost() != null ? request.unitCost() : BigDecimal.ZERO;
        if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Custo unitário não pode ser negativo");
        }

        StockEntryItem item = new StockEntryItem();
        item.setEntry(entry);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitCost(unitCost.setScale(4, RoundingMode.HALF_UP));
        item.setLineTotal(computeLineTotal(quantity, unitCost));
        itemRepository.save(item);

        return stockEntryMapper.toResponse(getEntity(entryId));
    }

    @Transactional
    public StockEntryResponse confirm(UUID entryId) {
        StockEntry entry = getEntity(entryId);
        assertStatus(entry, StockEntryStatus.DRAFT, "Confirmação");
        if (!itemRepository.existsByEntryIdAndActiveTrue(entryId)) {
            throw new BusinessRuleException("Entrada deve conter ao menos um item");
        }

        for (StockEntryItem item : entry.getItems()) {
            if (!Boolean.TRUE.equals(item.getActive())) {
                continue;
            }
            inventoryService.registerStockEntry(
                    item.getProduct().getId(),
                    entry.getWarehouse().getId(),
                    item.getQuantity(),
                    entry.getId(),
                    entry.getNotes());
        }

        entry.setStatus(StockEntryStatus.CONFIRMED);
        entry.setConfirmedAt(Instant.now());
        entryRepository.save(entry);
        audit(entry, AuditLog.AuditAction.STATUS_CHANGE, null, "Entrada de estoque confirmada");
        return stockEntryMapper.toResponse(getEntity(entryId));
    }

    @Transactional
    public StockEntryResponse cancel(UUID entryId) {
        StockEntry entry = getEntity(entryId);
        if (entry.getStatus() == StockEntryStatus.CONFIRMED) {
            throw new BusinessRuleException("Entrada confirmada não pode ser cancelada");
        }
        if (entry.getStatus() == StockEntryStatus.CANCELLED) {
            return stockEntryMapper.toResponse(entry);
        }
        entry.setStatus(StockEntryStatus.CANCELLED);
        entry.setCancelledAt(Instant.now());
        entryRepository.save(entry);
        audit(entry, AuditLog.AuditAction.STATUS_CHANGE, null, "Entrada de estoque cancelada");
        return stockEntryMapper.toResponse(getEntity(entryId));
    }

    private BigDecimal computeLineTotal(BigDecimal quantity, BigDecimal unitCost) {
        return quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
    }

    private void assertStatus(StockEntry entry, StockEntryStatus expected, String action) {
        if (entry.getStatus() != expected) {
            throw new BusinessRuleException(
                    "Ação '" + action + "' permitida apenas em entradas com status " + expected.name());
        }
    }

    private void assertSameOrganization(Organization organization, Store store, Warehouse warehouse) {
        UUID orgId = organization.getId();
        if (store.getOrganization() == null || !store.getOrganization().getId().equals(orgId)) {
            throw new BusinessRuleException("Loja não pertence à organização informada");
        }
        if (warehouse.getStore() == null
                || warehouse.getStore().getOrganization() == null
                || !warehouse.getStore().getOrganization().getId().equals(orgId)) {
            throw new BusinessRuleException("Depósito não pertence à organização informada");
        }
    }

    private void assertWarehouseBelongsToStore(Warehouse warehouse, Store store) {
        if (warehouse.getStore() == null || !warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
    }

    private String nextEntryNumber(UUID organizationId) {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "ENT-" + datePart + "-";
        long sequence = entryRepository.countByNumberPrefix(organizationId, prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private StockEntry getEntity(UUID id) {
        return entryRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de estoque", id));
    }

    private void audit(StockEntry entry, AuditLog.AuditAction action, Map<String, Object> before, String message) {
        domainAuditService.record(
                "STOCK_ENTRY",
                "StockEntry",
                entry.getId(),
                action,
                before,
                snapshot(entry),
                message);
    }

    private Map<String, Object> snapshot(StockEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("number", entry.getNumber());
        map.put("status", entry.getStatus().name());
        map.put("storeId", entry.getStore().getId());
        map.put("warehouseId", entry.getWarehouse().getId());
        return map;
    }
}
