package br.com.systemcommerce.fiscal.taxation.engine.service;

import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleConditionRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleConditionResponse;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleCreateRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleResponse;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleResultRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleResultResponse;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRule;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleCondition;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleResult;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxRuleConditionRepository;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxRuleRepository;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxRuleResultRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxRuleService {

    private final TaxRuleRepository taxRuleRepository;
    private final TaxRuleConditionRepository conditionRepository;
    private final TaxRuleResultRepository resultRepository;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<TaxRuleResponse> list(UUID organizationId) {
        return taxRuleRepository.findByOrganizationIdOrOrganizationIsNullOrderByPriorityDesc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxRuleResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public TaxRuleResponse create(TaxRuleCreateRequest request) {
        TaxRule rule = new TaxRule();
        if (request.organizationId() != null) {
            Organization organization = organizationService.requireUsable(request.organizationId());
            rule.setOrganization(organization);
        }
        rule.setCode(request.code());
        rule.setName(request.name());
        rule.setDescription(request.description());
        rule.setTaxKind(request.taxKind());
        rule.setPriority(request.priority());
        rule.setValidFrom(request.validFrom());
        rule.setValidUntil(request.validUntil());
        rule.setVersionCode(request.versionCode());
        TaxRule saved = taxRuleRepository.save(rule);

        saveConditions(saved, request.conditions());
        saveResults(saved, request.results());

        domainAuditService.record(
                "FISCAL",
                "TaxRule",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Regra tributária criada");
        return toResponse(getEntity(saved.getId()));
    }

    private TaxRule getEntity(UUID id) {
        return taxRuleRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regra tributária", id));
    }

    private void saveConditions(TaxRule rule, List<TaxRuleConditionRequest> conditions) {
        if (conditions == null) {
            return;
        }
        int order = 0;
        for (TaxRuleConditionRequest req : conditions) {
            TaxRuleCondition condition = new TaxRuleCondition();
            condition.setRule(rule);
            condition.setFieldName(req.fieldName());
            condition.setOperator(req.operator());
            condition.setValueText(req.valueText());
            condition.setSortOrder(req.sortOrder() != null ? req.sortOrder() : order++);
            conditionRepository.save(condition);
        }
    }

    private void saveResults(TaxRule rule, List<TaxRuleResultRequest> results) {
        if (results == null) {
            return;
        }
        for (TaxRuleResultRequest req : results) {
            TaxRuleResult result = new TaxRuleResult();
            result.setRule(rule);
            result.setResultKey(req.resultKey());
            result.setResultValue(req.resultValue());
            result.setNumericValue(req.numericValue());
            resultRepository.save(result);
        }
    }

    private TaxRuleResponse toResponse(TaxRule rule) {
        List<TaxRuleConditionResponse> conditions =
                conditionRepository.findByRuleIdOrderBySortOrder(rule.getId()).stream()
                        .map(c -> new TaxRuleConditionResponse(
                                c.getId(),
                                c.getFieldName(),
                                c.getOperator().name(),
                                c.getValueText(),
                                c.getSortOrder()))
                        .toList();
        List<TaxRuleResultResponse> results = resultRepository.findByRuleId(rule.getId()).stream()
                .map(r -> new TaxRuleResultResponse(r.getId(), r.getResultKey(), r.getResultValue(), r.getNumericValue()))
                .toList();
        return new TaxRuleResponse(
                rule.getId(),
                rule.getOrganization() != null ? rule.getOrganization().getId() : null,
                rule.getCode(),
                rule.getName(),
                rule.getDescription(),
                rule.getTaxKind(),
                rule.getPriority(),
                rule.getStatus(),
                rule.getValidFrom(),
                rule.getValidUntil(),
                rule.getVersionCode(),
                conditions,
                results,
                rule.getCreatedAt());
    }

    private Map<String, Object> snapshot(TaxRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("code", rule.getCode());
        map.put("taxKind", rule.getTaxKind());
        map.put("priority", rule.getPriority());
        return map;
    }
}
