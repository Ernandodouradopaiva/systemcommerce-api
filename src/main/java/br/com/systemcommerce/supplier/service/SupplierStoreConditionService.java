package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierStoreConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierStoreConditionResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierStoreCondition;
import br.com.systemcommerce.supplier.mapper.SupplierStoreConditionMapper;
import br.com.systemcommerce.supplier.repository.SupplierStoreConditionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Observações/condições por loja — apenas registro; nunca decide autorização ou totais oficiais. */
@Service
@RequiredArgsConstructor
public class SupplierStoreConditionService {

    private final SupplierStoreConditionRepository storeConditionRepository;
    private final SupplierStoreConditionMapper storeConditionMapper;
    private final StoreRepository storeRepository;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierStoreConditionResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return storeConditionRepository.findBySupplierIdOrderByCreatedAtAsc(supplierId).stream()
                .map(storeConditionMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierStoreConditionResponse create(UUID supplierId, SupplierStoreConditionRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        if (storeConditionRepository.existsBySupplierIdAndStoreId(supplierId, request.storeId())) {
            throw new ConflictException("Já existe condição cadastrada para esta loja");
        }
        Store store = requireStore(request.storeId());
        SupplierStoreCondition condition = new SupplierStoreCondition();
        condition.setSupplier(supplier);
        condition.setStore(store);
        storeConditionMapper.apply(condition, request);
        SupplierStoreCondition saved = storeConditionRepository.save(condition);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierStoreCondition",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Condição por loja criada para o fornecedor");
        return storeConditionMapper.toResponse(saved);
    }

    @Transactional
    public SupplierStoreConditionResponse update(
            UUID supplierId, UUID conditionId, SupplierStoreConditionRequest request) {
        SupplierStoreCondition condition = getOwned(supplierId, conditionId);
        if (!condition.getStore().getId().equals(request.storeId())
                && storeConditionRepository.existsBySupplierIdAndStoreIdAndIdNot(
                        supplierId, request.storeId(), conditionId)) {
            throw new ConflictException("Já existe condição cadastrada para esta loja");
        }
        if (!condition.getStore().getId().equals(request.storeId())) {
            condition.setStore(requireStore(request.storeId()));
        }
        storeConditionMapper.apply(condition, request);
        SupplierStoreCondition saved = storeConditionRepository.save(condition);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierStoreCondition",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Condição por loja atualizada para o fornecedor");
        return storeConditionMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID conditionId) {
        SupplierStoreCondition condition = getOwned(supplierId, conditionId);
        storeConditionRepository.delete(condition);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierStoreCondition",
                conditionId,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Condição por loja removida do fornecedor");
    }

    private Store requireStore(UUID storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Loja", storeId));
    }

    private SupplierStoreCondition getOwned(UUID supplierId, UUID conditionId) {
        SupplierStoreCondition condition = storeConditionRepository
                .findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condição por loja do fornecedor", conditionId));
        if (!condition.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Condição por loja do fornecedor", conditionId);
        }
        return condition;
    }
}
