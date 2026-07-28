package br.com.systemcommerce.pos.terminal.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalLinkWarehouseRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalResponse;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalUpdateRequest;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.mapper.PosTerminalMapper;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.pos.terminal.specification.PosTerminalSpecifications;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Instant;
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
public class PosTerminalService {

    private final PosTerminalRepository posTerminalRepository;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final PosTerminalMapper posTerminalMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<PosTerminalResponse> list(
            UUID storeId, UUID warehouseId, PosTerminal.TerminalStatus status, String search, Pageable pageable) {
        return posTerminalRepository
                .findAll(PosTerminalSpecifications.withFilters(storeId, warehouseId, status, search), pageable)
                .map(posTerminalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PosTerminalResponse> listAvailable(UUID storeId, Pageable pageable) {
        return posTerminalRepository
                .findAll(PosTerminalSpecifications.availableForCashOpen(storeId), pageable)
                .map(posTerminalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PosTerminalResponse getById(UUID id) {
        return posTerminalMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PosTerminalResponse create(PosTerminalCreateRequest request) {
        Store store = storeService.requireUsable(request.storeId());
        Warehouse warehouse = resolveWarehouseForStore(store, request.warehouseId());
        assertUniqueCode(store.getId(), request.code(), null);
        assertUniqueNumber(store.getId(), request.terminalNumber(), null);

        PosTerminal terminal = new PosTerminal();
        posTerminalMapper.applyCreate(terminal, request, store, warehouse);
        PosTerminal saved = posTerminalRepository.save(terminal);
        domainAuditService.record(
                "POS",
                "PosTerminal",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Terminal PDV criado");
        return posTerminalMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public PosTerminalResponse update(UUID id, PosTerminalUpdateRequest request) {
        PosTerminal terminal = getEntity(id);
        Map<String, Object> before = snapshot(terminal);
        assertUniqueCode(terminal.getStore().getId(), request.code(), id);
        assertUniqueNumber(terminal.getStore().getId(), request.terminalNumber(), id);
        posTerminalMapper.applyUpdate(terminal, request);
        PosTerminal saved = posTerminalRepository.save(terminal);
        domainAuditService.record(
                "POS",
                "PosTerminal",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Terminal PDV atualizado");
        return posTerminalMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PosTerminalResponse linkWarehouse(UUID id, PosTerminalLinkWarehouseRequest request) {
        PosTerminal terminal = getEntity(id);
        Map<String, Object> before = snapshot(terminal);
        Warehouse warehouse = resolveWarehouseForStore(terminal.getStore(), request.warehouseId());
        terminal.setWarehouse(warehouse);
        PosTerminal saved = posTerminalRepository.save(terminal);
        domainAuditService.record(
                "POS",
                "PosTerminal",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Terminal vinculado a depósito");
        return posTerminalMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PosTerminalResponse activate(UUID id) {
        PosTerminal terminal = getEntity(id);
        if (!terminal.getStore().isUsable()) {
            throw new BusinessRuleException("Não é possível ativar terminal de loja inativa");
        }
        if (!terminal.getWarehouse().isEligibleForPosSale()) {
            throw new BusinessRuleException(
                    "Não é possível ativar terminal sem depósito ativo autorizado para venda");
        }
        Map<String, Object> before = snapshot(terminal);
        terminal.markActive();
        PosTerminal saved = posTerminalRepository.save(terminal);
        domainAuditService.record(
                "POS",
                "PosTerminal",
                id,
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Terminal PDV ativado");
        return posTerminalMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PosTerminalResponse inactivate(UUID id) {
        PosTerminal terminal = getEntity(id);
        Map<String, Object> before = snapshot(terminal);
        terminal.markInactive();
        PosTerminal saved = posTerminalRepository.save(terminal);
        domainAuditService.record(
                "POS",
                "PosTerminal",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Terminal PDV inativado");
        return posTerminalMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PosTerminalResponse touchCommunication(UUID id) {
        PosTerminal terminal = getEntity(id);
        terminal.setLastCommunicationAt(Instant.now());
        return posTerminalMapper.toResponse(posTerminalRepository.save(terminal));
    }

    /**
     * Valida elegibilidade para abertura de caixa.
     * A unicidade de sessão OPEN por terminal será enforced na tabela cash_sessions.
     */
    @Transactional(readOnly = true)
    public PosTerminal requireEligibleToOpenCashSession(UUID terminalId) {
        PosTerminal terminal = getEntity(terminalId);
        if (!terminal.isUsable()) {
            throw new BusinessRuleException("Terminal inativo não pode abrir caixa");
        }
        storeService.requireAllowsPos(terminal.getStore().getId());
        if (!terminal.getWarehouse().isEligibleForPosSale()) {
            throw new BusinessRuleException(
                    "Terminal deve estar associado a um depósito ativo autorizado para venda");
        }
        return terminal;
    }

    @Transactional(readOnly = true)
    public PosTerminal getEntity(UUID id) {
        return posTerminalRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal PDV", id));
    }

    private Warehouse resolveWarehouseForStore(Store store, UUID warehouseId) {
        Warehouse warehouse = warehouseService.requireEligibleForPosSale(warehouseId);
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito deve pertencer à mesma loja do terminal");
        }
        return warehouse;
    }

    private void assertUniqueCode(UUID storeId, String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? posTerminalRepository.existsByStoreIdAndCodeIgnoreCase(storeId, normalized)
                : posTerminalRepository.existsByStoreIdAndCodeIgnoreCaseAndIdNot(storeId, normalized, id);
        if (exists) {
            throw new ConflictException("Código do terminal já está em uso nesta loja");
        }
    }

    private void assertUniqueNumber(UUID storeId, Integer terminalNumber, UUID id) {
        if (terminalNumber == null || terminalNumber < 1) {
            throw new BusinessRuleException("Número do terminal deve ser >= 1");
        }
        boolean exists = id == null
                ? posTerminalRepository.existsByStoreIdAndTerminalNumber(storeId, terminalNumber)
                : posTerminalRepository.existsByStoreIdAndTerminalNumberAndIdNot(storeId, terminalNumber, id);
        if (exists) {
            throw new ConflictException("Número do terminal já está em uso nesta loja");
        }
    }

    private Map<String, Object> snapshot(PosTerminal terminal) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", terminal.getId());
        map.put("storeId", terminal.getStore().getId());
        map.put("warehouseId", terminal.getWarehouse().getId());
        map.put("code", terminal.getCode());
        map.put("name", terminal.getName());
        map.put("terminalNumber", terminal.getTerminalNumber());
        map.put("status", terminal.getStatus());
        map.put("printModel", terminal.getPrintModel());
        map.put("active", terminal.getActive());
        map.put("eligibleToOpenCashSession", terminal.isEligibleToOpenCashSession());
        return map;
    }
}
