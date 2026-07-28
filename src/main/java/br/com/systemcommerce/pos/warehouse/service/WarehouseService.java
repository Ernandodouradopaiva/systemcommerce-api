package br.com.systemcommerce.pos.warehouse.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseUpdateRequest;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.mapper.WarehouseMapper;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRepository;
import br.com.systemcommerce.pos.warehouse.specification.WarehouseSpecifications;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
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
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final StoreService storeService;
    private final WarehouseMapper warehouseMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> list(
            UUID storeId, Warehouse.WarehouseStatus status, Boolean allowsSale, String search, Pageable pageable) {
        return warehouseRepository
                .findAll(WarehouseSpecifications.withFilters(storeId, status, allowsSale, search), pageable)
                .map(warehouseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID id) {
        return warehouseMapper.toResponse(getEntity(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseCreateRequest request) {
        Store store = storeService.requireUsable(request.storeId());
        assertUniqueCode(store.getId(), request.code(), null);
        Warehouse warehouse = new Warehouse();
        warehouseMapper.applyCreate(warehouse, request, store);
        Warehouse saved = warehouseRepository.save(warehouse);
        domainAuditService.record(
                "WAREHOUSE",
                "Warehouse",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Depósito criado");
        return warehouseMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public WarehouseResponse update(UUID id, WarehouseUpdateRequest request) {
        Warehouse warehouse = getEntity(id);
        Map<String, Object> before = snapshot(warehouse);
        assertUniqueCode(warehouse.getStore().getId(), request.code(), id);
        warehouseMapper.applyUpdate(warehouse, request);
        Warehouse saved = warehouseRepository.save(warehouse);
        domainAuditService.record(
                "WAREHOUSE",
                "Warehouse",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Depósito atualizado");
        return warehouseMapper.toResponse(getEntity(id));
    }

    @Transactional
    public WarehouseResponse activate(UUID id) {
        Warehouse warehouse = getEntity(id);
        Map<String, Object> before = snapshot(warehouse);
        warehouse.markActive();
        Warehouse saved = warehouseRepository.save(warehouse);
        domainAuditService.record(
                "WAREHOUSE",
                "Warehouse",
                id,
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Depósito ativado");
        return warehouseMapper.toResponse(getEntity(id));
    }

    @Transactional
    public WarehouseResponse inactivate(UUID id) {
        Warehouse warehouse = getEntity(id);
        if (posTerminalRepository.existsByWarehouseId(id)) {
            // permite inativar, mas terminais deixam de ser elegíveis
        }
        Map<String, Object> before = snapshot(warehouse);
        warehouse.markInactive();
        Warehouse saved = warehouseRepository.save(warehouse);
        domainAuditService.record(
                "WAREHOUSE",
                "Warehouse",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Depósito inativado");
        return warehouseMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Warehouse requireEligibleForPosSale(UUID warehouseId) {
        Warehouse warehouse = getEntity(warehouseId);
        if (!warehouse.isEligibleForPosSale()) {
            throw new BusinessRuleException(
                    "Depósito deve estar ativo, permitir venda e pertencer a loja ativa");
        }
        return warehouse;
    }

    @Transactional(readOnly = true)
    public Warehouse requireUsable(UUID warehouseId) {
        Warehouse warehouse = getEntity(warehouseId);
        if (!warehouse.isUsable()) {
            throw new BusinessRuleException("Depósito inativo não pode ser utilizado");
        }
        return warehouse;
    }

    /** Depósito padrão (seed LOJA-01/DEP-01) para operações legadas sem warehouse explícito. */
    @Transactional(readOnly = true)
    public Warehouse requireDefaultWarehouse() {
        return warehouseRepository
                .findDefaultSeedWarehouse()
                .orElseThrow(() -> new BusinessRuleException(
                        "Depósito padrão (LOJA-01/DEP-01) não encontrado. Execute as migrations de seed."));
    }

    @Transactional(readOnly = true)
    public Warehouse getEntity(UUID id) {
        return warehouseRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito", id));
    }

    private void assertUniqueCode(UUID storeId, String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? warehouseRepository.existsByStoreIdAndCodeIgnoreCase(storeId, normalized)
                : warehouseRepository.existsByStoreIdAndCodeIgnoreCaseAndIdNot(storeId, normalized, id);
        if (exists) {
            throw new ConflictException("Código do depósito já está em uso nesta loja");
        }
    }

    private Map<String, Object> snapshot(Warehouse warehouse) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", warehouse.getId());
        map.put("storeId", warehouse.getStore().getId());
        map.put("code", warehouse.getCode());
        map.put("name", warehouse.getName());
        map.put("allowsSale", warehouse.getAllowsSale());
        map.put("status", warehouse.getStatus());
        map.put("active", warehouse.getActive());
        return map;
    }
}
