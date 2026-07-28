package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierCommercialCondition;
import br.com.systemcommerce.supplier.mapper.SupplierCommercialConditionMapper;
import br.com.systemcommerce.supplier.repository.SupplierCommercialConditionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Condições comerciais padrão (nível organização) — servem como referência; totais reais ficam no pedido de compra. */
@Service
@RequiredArgsConstructor
public class SupplierCommercialConditionService {

    private final SupplierCommercialConditionRepository conditionRepository;
    private final SupplierCommercialConditionMapper conditionMapper;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Optional<SupplierCommercialConditionResponse> get(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return conditionRepository.findBySupplierId(supplierId).map(conditionMapper::toResponse);
    }

    @Transactional
    public SupplierCommercialConditionResponse upsert(UUID supplierId, SupplierCommercialConditionRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        SupplierCommercialCondition condition = conditionRepository
                .findBySupplierId(supplierId)
                .orElseGet(() -> {
                    SupplierCommercialCondition created = new SupplierCommercialCondition();
                    created.setSupplier(supplier);
                    return created;
                });
        boolean isNew = condition.getId() == null;
        conditionMapper.apply(condition, request);
        SupplierCommercialCondition saved = conditionRepository.save(condition);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierCommercialCondition",
                saved.getId(),
                isNew ? AuditLog.AuditAction.CREATE : AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Condições comerciais padrão do fornecedor " + (isNew ? "criadas" : "atualizadas"));
        return conditionMapper.toResponse(saved);
    }
}
