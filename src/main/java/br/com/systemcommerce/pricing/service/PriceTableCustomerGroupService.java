package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.PriceTableCustomerGroupRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCustomerGroupResponse;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.entity.PriceTableCustomerGroup;
import br.com.systemcommerce.pricing.repository.PriceTableCustomerGroupRepository;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Restrição opcional de tabela de preço a grupos de cliente (Prompt 68). */
@Service
@RequiredArgsConstructor
public class PriceTableCustomerGroupService {

    private final PriceTableCustomerGroupRepository repository;
    private final PriceTableRepository priceTableRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<PriceTableCustomerGroupResponse> list(UUID priceTableId) {
        requirePriceTable(priceTableId);
        return repository.findByPriceTable_IdAndActiveTrue(priceTableId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PriceTableCustomerGroupResponse create(UUID priceTableId, PriceTableCustomerGroupRequest request) {
        PriceTable table = requirePriceTable(priceTableId);
        String code = MoneyAndQuantityUtils.requireText(request.customerGroupCode(), "Código do grupo");
        if (repository.existsByPriceTable_IdAndCustomerGroupCodeIgnoreCase(priceTableId, code)) {
            throw new ConflictException("Grupo de cliente já vinculado a esta tabela de preço");
        }
        PriceTableCustomerGroup group = new PriceTableCustomerGroup();
        group.setPriceTable(table);
        group.setCustomerGroupCode(code);
        group.setCustomerGroupName(MoneyAndQuantityUtils.blankToNull(request.customerGroupName()));
        PriceTableCustomerGroup saved = repository.save(group);
        domainAuditService.record(
                "PRICING",
                "PriceTableCustomerGroup",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Grupo de cliente vinculado à tabela de preço");
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID priceTableId, UUID groupId) {
        PriceTableCustomerGroup group = repository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de cliente da tabela", groupId));
        if (!group.getPriceTable().getId().equals(priceTableId)) {
            throw new ResourceNotFoundException("Grupo de cliente da tabela", groupId);
        }
        repository.delete(group);
        domainAuditService.record(
                "PRICING",
                "PriceTableCustomerGroup",
                groupId,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Grupo de cliente desvinculado da tabela de preço");
    }

    private PriceTable requirePriceTable(UUID id) {
        return priceTableRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tabela de preço", id));
    }

    private PriceTableCustomerGroupResponse toResponse(PriceTableCustomerGroup group) {
        return new PriceTableCustomerGroupResponse(
                group.getId(),
                group.getPriceTable().getId(),
                group.getCustomerGroupCode(),
                group.getCustomerGroupName(),
                group.getActive());
    }
}
