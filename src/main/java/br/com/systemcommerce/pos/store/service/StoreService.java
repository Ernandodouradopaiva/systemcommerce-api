package br.com.systemcommerce.pos.store.service;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.dto.StoreResponse;
import br.com.systemcommerce.pos.store.dto.StoreSummaryResponse;
import br.com.systemcommerce.pos.store.dto.StoreUpdateRequest;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.mapper.StoreMapper;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.store.specification.StoreSpecifications;
import br.com.systemcommerce.pos.store.support.PendingStoreTransferQuery;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final CashSessionRepository cashSessionRepository;
    private final OrganizationService organizationService;
    private final PendingStoreTransferQuery pendingStoreTransferQuery;
    private final StoreMapper storeMapper;
    private final DomainAuditService domainAuditService;

    @Value("${app.multistore.allow-multiple-headquarters:false}")
    private boolean allowMultipleHeadquarters;

    @Transactional(readOnly = true)
    public Page<StoreResponse> list(
            UUID organizationId,
            String code,
            Store.StoreStatus status,
            Store.EstablishmentType establishmentType,
            Boolean headquarters,
            Boolean allowsSales,
            Boolean allowsPos,
            String search,
            Pageable pageable) {
        return storeRepository
                .findAll(
                        StoreSpecifications.withFilters(
                                organizationId,
                                code,
                                status,
                                establishmentType,
                                headquarters,
                                allowsSales,
                                allowsPos,
                                search),
                        pageable)
                .map(storeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> search(String search, UUID organizationId, Pageable pageable) {
        return list(organizationId, null, null, null, null, null, null, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> listOperational(UUID organizationId, Pageable pageable) {
        var spec = StoreSpecifications.operational();
        if (organizationId != null) {
            spec = spec.and(StoreSpecifications.withFilters(
                    organizationId, null, null, null, null, null, null, null));
        }
        return storeRepository.findAll(spec, pageable).map(storeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StoreResponse getById(UUID id) {
        return storeMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public StoreSummaryResponse getSummary(UUID id) {
        Store store = getEntity(id);
        long openSessions = cashSessionRepository.countByStoreIdAndStatusIn(
                id, EnumSet.of(CashSession.CashSessionStatus.OPEN, CashSession.CashSessionStatus.CLOSING));
        return new StoreSummaryResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getTradeName(),
                store.getStatus(),
                store.isHeadquarters(),
                store.isAllowsSales(),
                store.isAllowsPos(),
                warehouseRepository.countByStoreId(store.getId()),
                posTerminalRepository.countByStoreId(store.getId()),
                openSessions);
    }

    @Transactional
    public StoreResponse create(StoreCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUniqueCode(organization.getId(), request.code(), null);
        assertUniqueDocument(request.document(), null);
        Store store = new Store();
        store.setOrganization(organization);
        storeMapper.applyCreate(store, request);
        enforceHeadquartersRules(store, true);
        Store saved = storeRepository.save(store);
        domainAuditService.record(
                "STORE", "Store", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Loja criada");
        return storeMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public StoreResponse update(UUID id, StoreUpdateRequest request) {
        Store store = getEntity(id);
        Map<String, Object> before = snapshot(store);
        assertUniqueCode(store.getOrganization().getId(), request.code(), id);
        assertUniqueDocument(request.document(), id);
        storeMapper.applyUpdate(store, request);
        enforceHeadquartersRules(store, false);
        Store saved = storeRepository.save(store);
        domainAuditService.record(
                "STORE", "Store", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Loja atualizada");
        return storeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StoreResponse activate(UUID id) {
        Store store = getEntity(id);
        Map<String, Object> before = snapshot(store);
        store.markActive();
        Store saved = storeRepository.save(store);
        domainAuditService.record(
                "STORE", "Store", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Loja ativada");
        return storeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StoreResponse inactivate(UUID id) {
        Store store = getEntity(id);
        assertSafeToInactivate(store);
        Map<String, Object> before = snapshot(store);
        store.markInactive();
        Store saved = storeRepository.save(store);
        domainAuditService.record(
                "STORE", "Store", id, AuditLog.AuditAction.DEACTIVATE, before, snapshot(saved), "Loja inativada");
        return storeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StoreResponse defineHeadquarters(UUID id) {
        Store store = getEntity(id);
        if (!store.isUsable()) {
            throw new BusinessRuleException("Loja inativa não pode ser definida como matriz");
        }
        Map<String, Object> before = snapshot(store);
        UUID organizationId = store.getOrganization().getId();
        if (!allowMultipleHeadquarters) {
            clearOtherHeadquarters(organizationId, id);
        } else if (storeRepository.existsByOrganizationIdAndHeadquartersTrueAndIdNot(organizationId, id)
                && !allowMultipleHeadquarters) {
            throw new BusinessRuleException(
                    "Já existe uma loja matriz nesta organização; habilite app.multistore.allow-multiple-headquarters");
        }
        store.setHeadquarters(true);
        store.setEstablishmentType(Store.EstablishmentType.HEADQUARTERS);
        Store saved = storeRepository.save(store);
        domainAuditService.record(
                "STORE",
                "Store",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Loja definida como matriz");
        return storeMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Store requireUsable(UUID storeId) {
        Store store = getEntity(storeId);
        if (!store.isUsable()) {
            throw new BusinessRuleException("Loja inativa não pode ser utilizada");
        }
        return store;
    }

    @Transactional(readOnly = true)
    public Store requireAllowsSales(UUID storeId) {
        Store store = requireUsable(storeId);
        if (!store.isAllowsSales()) {
            throw new BusinessRuleException("Loja não permite novas vendas");
        }
        return store;
    }

    @Transactional(readOnly = true)
    public Store requireAllowsPos(UUID storeId) {
        Store store = requireUsable(storeId);
        if (!store.isAllowsPos()) {
            throw new BusinessRuleException("Loja não permite operação de PDV / abertura de caixa");
        }
        return store;
    }

    @Transactional(readOnly = true)
    public Store getEntity(UUID id) {
        return storeRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loja", id));
    }

    private void assertSafeToInactivate(Store store) {
        long openSessions = cashSessionRepository.countByStoreIdAndStatusIn(
                store.getId(),
                EnumSet.of(CashSession.CashSessionStatus.OPEN, CashSession.CashSessionStatus.CLOSING));
        if (openSessions > 0) {
            throw new BusinessRuleException(
                    "Não é possível inativar a loja com sessões de caixa abertas ou em fechamento");
        }
        if (pendingStoreTransferQuery.hasPendingTransfers(store.getId())) {
            throw new BusinessRuleException("Não é possível inativar a loja com transferências de estoque pendentes");
        }
    }

    private void enforceHeadquartersRules(Store store, boolean creating) {
        if (store.getEstablishmentType() == Store.EstablishmentType.HEADQUARTERS) {
            store.setHeadquarters(true);
        }
        if (!store.isHeadquarters()) {
            return;
        }
        store.setEstablishmentType(Store.EstablishmentType.HEADQUARTERS);
        UUID organizationId = store.getOrganization().getId();
        UUID storeId = store.getId();
        boolean anotherExists = creating || storeId == null
                ? storeRepository.existsByOrganizationIdAndHeadquartersTrue(organizationId)
                : storeRepository.existsByOrganizationIdAndHeadquartersTrueAndIdNot(organizationId, storeId);
        if (anotherExists && !allowMultipleHeadquarters) {
            if (creating || storeId == null) {
                throw new BusinessRuleException(
                        "Já existe uma loja matriz nesta organização; use definir matriz ou permita múltiplas matrizes na configuração");
            }
            clearOtherHeadquarters(organizationId, storeId);
        }
    }

    private void clearOtherHeadquarters(UUID organizationId, UUID keepStoreId) {
        List<Store> stores = storeRepository.findAll(
                StoreSpecifications.withFilters(organizationId, null, null, null, true, null, null, null));
        for (Store other : stores) {
            if (!other.getId().equals(keepStoreId) && other.isHeadquarters()) {
                other.setHeadquarters(false);
                if (other.getEstablishmentType() == Store.EstablishmentType.HEADQUARTERS) {
                    other.setEstablishmentType(Store.EstablishmentType.BRANCH);
                }
                storeRepository.save(other);
            }
        }
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? storeRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, normalized)
                : storeRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, normalized, id);
        if (exists) {
            throw new ConflictException("Código da loja já está em uso nesta organização");
        }
    }

    private void assertUniqueDocument(String document, UUID id) {
        if (!StringUtils.hasText(document)) {
            return;
        }
        String normalized = document.replaceAll("\\D", "");
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        boolean exists = id == null
                ? storeRepository.existsByDocumentIgnoreCase(normalized)
                : storeRepository.existsByDocumentIgnoreCaseAndIdNot(normalized, id);
        if (exists) {
            throw new ConflictException("CNPJ da loja já está em uso");
        }
    }

    private Map<String, Object> snapshot(Store store) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", store.getId());
        map.put("organizationId", store.getOrganization() != null ? store.getOrganization().getId() : null);
        map.put("code", store.getCode());
        map.put("name", store.getName());
        map.put("tradeName", store.getTradeName());
        map.put("document", store.getDocument());
        map.put("establishmentType", store.getEstablishmentType());
        map.put("headquarters", store.isHeadquarters());
        map.put("allowsSales", store.isAllowsSales());
        map.put("allowsPos", store.isAllowsPos());
        map.put("status", store.getStatus());
        map.put("active", store.getActive());
        map.put("timezone", store.getTimezone());
        map.put("hasWarehouses", warehouseRepository.existsByStoreId(store.getId()));
        map.put("hasTerminals", posTerminalRepository.existsByStoreId(store.getId()));
        return map;
    }
}
