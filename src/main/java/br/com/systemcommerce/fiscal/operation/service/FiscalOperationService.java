package br.com.systemcommerce.fiscal.operation.service;

import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationCreateRequest;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationItemRuleRequest;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationItemRuleResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationResolvedResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationRuleRequest;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationRuleResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationUpdateRequest;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationItemRule;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationRule;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperationStoreAssignment;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationItemRuleRepository;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationRepository;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationRuleRepository;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationStoreAssignmentRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalOperationService {

    private final FiscalOperationRepository operationRepository;
    private final FiscalOperationRuleRepository ruleRepository;
    private final FiscalOperationItemRuleRepository itemRuleRepository;
    private final FiscalOperationStoreAssignmentRepository storeAssignmentRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<FiscalOperationResponse> list(UUID organizationId) {
        return operationRepository.findByOrganizationIdOrderByCode(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalOperationResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public FiscalOperationResolvedResponse resolve(
            String operationCode,
            UUID organizationId,
            UUID storeId,
            String originUf,
            String destUf,
            Boolean finalConsumer,
            String model) {
        FiscalOperation operation = operationRepository
                .findByOrganizationIdAndCode(organizationId, operationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Operação fiscal", operationCode));

        if (!operation.isValidOn(LocalDate.now())) {
            throw new BusinessRuleException("Operação fiscal não válida na data atual");
        }

        validateModel(operation, model);
        assertStoreAllowed(operation, storeId);

        String normalizedOrigin = normalizeUf(originUf);
        String normalizedDest = normalizeUf(destUf);
        boolean isFinalConsumer = Boolean.TRUE.equals(finalConsumer);

        List<FiscalOperationRule> rules = ruleRepository.findByOperationIdOrderByPriorityDesc(operation.getId());
        FiscalOperationRule matched = rules.stream()
                .filter(r -> r.getStatus() == FiscalOperationRule.RuleStatus.ACTIVE)
                .filter(r -> matchesRule(r, normalizedOrigin, normalizedDest, isFinalConsumer))
                .findFirst()
                .orElse(null);

        String cfop = matched != null && StringUtils.hasText(matched.getCfop())
                ? matched.getCfop()
                : operation.getDefaultCfop();
        String taxRuleCode = matched != null ? matched.getTaxRuleCode() : null;

        return new FiscalOperationResolvedResponse(
                operation.getId(),
                operation.getCode(),
                cfop,
                operation.getNatureOfOperation(),
                operation.getPurpose(),
                taxRuleCode,
                operation.getGeneratesFinance(),
                operation.getMovesStock(),
                operation.getStockDirection(),
                operation.getRequiresReferencedDocument());
    }

    @Transactional
    public FiscalOperationResponse create(FiscalOperationCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        operationRepository
                .findByOrganizationIdAndCode(organization.getId(), request.code())
                .ifPresent(o -> {
                    throw new ConflictException("Já existe operação fiscal com este código");
                });

        FiscalOperation operation = new FiscalOperation();
        operation.setOrganization(organization);
        operation.setCode(request.code());
        applyFields(operation, request);
        FiscalOperation saved = operationRepository.save(operation);

        saveRules(saved, request.rules());
        saveItemRules(saved, request.itemRules());
        saveStoreAssignments(saved, request.storeIds(), organization.getId());

        domainAuditService.record(
                "FISCAL",
                "FiscalOperation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Operação fiscal criada");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public FiscalOperationResponse update(UUID id, FiscalOperationUpdateRequest request) {
        FiscalOperation operation = getEntity(id);
        Map<String, Object> before = snapshot(operation);
        applyFields(operation, request);
        FiscalOperation saved = operationRepository.save(operation);

        if (request.rules() != null) {
            ruleRepository.findByOperationIdOrderByPriorityDesc(id).forEach(r -> {
                r.setActive(false);
                ruleRepository.save(r);
            });
            saveRules(saved, request.rules());
        }
        if (request.itemRules() != null) {
            itemRuleRepository.findByOperationId(id).forEach(r -> {
                r.setActive(false);
                itemRuleRepository.save(r);
            });
            saveItemRules(saved, request.itemRules());
        }
        if (request.storeIds() != null) {
            storeAssignmentRepository.findByOperationId(id).forEach(a -> {
                a.setActive(false);
                storeAssignmentRepository.save(a);
            });
            saveStoreAssignments(saved, request.storeIds(), operation.getOrganization().getId());
        }

        domainAuditService.record(
                "FISCAL",
                "FiscalOperation",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Operação fiscal atualizada");
        return toResponse(getEntity(saved.getId()));
    }

    public void validateModel(FiscalOperation operation, String model) {
        if (!StringUtils.hasText(model) || !StringUtils.hasText(operation.getAllowedModels())) {
            return;
        }
        List<String> allowed = List.of(operation.getAllowedModels().split(","));
        boolean ok = allowed.stream().map(String::trim).anyMatch(m -> m.equals(model.trim()));
        if (!ok) {
            throw new BusinessRuleException("Modelo " + model + " não permitido para esta operação");
        }
    }

    private boolean matchesRule(
            FiscalOperationRule rule, String originUf, String destUf, boolean finalConsumer) {
        if (StringUtils.hasText(rule.getOriginUf()) && !rule.getOriginUf().equalsIgnoreCase(originUf)) {
            return false;
        }
        if (StringUtils.hasText(rule.getDestUf()) && !rule.getDestUf().equalsIgnoreCase(destUf)) {
            return false;
        }
        if (rule.getFinalConsumer() != null && rule.getFinalConsumer() != finalConsumer) {
            return false;
        }
        return true;
    }

    private void assertStoreAllowed(FiscalOperation operation, UUID storeId) {
        List<FiscalOperationStoreAssignment> assignments =
                storeAssignmentRepository.findByOperationId(operation.getId()).stream()
                        .filter(a -> a.getStatus() == FiscalOperationStoreAssignment.AssignmentStatus.ACTIVE)
                        .filter(a -> Boolean.TRUE.equals(a.getActive()))
                        .toList();
        if (assignments.isEmpty()) {
            return;
        }
        if (storeId == null) {
            throw new BusinessRuleException("Operação fiscal restrita a lojas específicas");
        }
        boolean allowed = assignments.stream()
                .anyMatch(a -> a.getStore().getId().equals(storeId));
        if (!allowed) {
            throw new BusinessRuleException("Operação fiscal não disponível para esta loja");
        }
    }

    private FiscalOperation getEntity(UUID id) {
        return operationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operação fiscal", id));
    }

    private void applyFields(FiscalOperation operation, FiscalOperationCreateRequest request) {
        operation.setName(request.name());
        operation.setNatureOfOperation(request.natureOfOperation());
        operation.setPurpose(request.purpose());
        operation.setAllowedModels(request.allowedModels());
        operation.setDefaultCfop(request.defaultCfop());
        operation.setGeneratesFinance(
                request.generatesFinance() != null ? request.generatesFinance() : Boolean.FALSE);
        operation.setMovesStock(request.movesStock() != null ? request.movesStock() : Boolean.FALSE);
        operation.setStockDirection(
                request.stockDirection() != null ? request.stockDirection() : FiscalOperation.StockDirection.NONE);
        operation.setRequiresReferencedDocument(
                request.requiresReferencedDocument() != null ? request.requiresReferencedDocument() : Boolean.FALSE);
        operation.setAllowsFinalConsumer(
                request.allowsFinalConsumer() != null ? request.allowsFinalConsumer() : Boolean.TRUE);
        operation.setValidFrom(request.validFrom());
        operation.setValidUntil(request.validUntil());
    }

    private void applyFields(FiscalOperation operation, FiscalOperationUpdateRequest request) {
        operation.setName(request.name());
        operation.setNatureOfOperation(request.natureOfOperation());
        operation.setPurpose(request.purpose());
        operation.setAllowedModels(request.allowedModels());
        operation.setDefaultCfop(request.defaultCfop());
        if (request.generatesFinance() != null) {
            operation.setGeneratesFinance(request.generatesFinance());
        }
        if (request.movesStock() != null) {
            operation.setMovesStock(request.movesStock());
        }
        if (request.stockDirection() != null) {
            operation.setStockDirection(request.stockDirection());
        }
        if (request.requiresReferencedDocument() != null) {
            operation.setRequiresReferencedDocument(request.requiresReferencedDocument());
        }
        if (request.allowsFinalConsumer() != null) {
            operation.setAllowsFinalConsumer(request.allowsFinalConsumer());
        }
        operation.setValidFrom(request.validFrom());
        operation.setValidUntil(request.validUntil());
    }

    private void saveRules(FiscalOperation operation, List<FiscalOperationRuleRequest> rules) {
        if (rules == null) {
            return;
        }
        for (FiscalOperationRuleRequest req : rules) {
            FiscalOperationRule rule = new FiscalOperationRule();
            rule.setOperation(operation);
            rule.setOriginUf(normalizeUf(req.originUf()));
            rule.setDestUf(normalizeUf(req.destUf()));
            rule.setTaxpayerIndicator(req.taxpayerIndicator());
            rule.setFinalConsumer(req.finalConsumer());
            rule.setCfop(req.cfop());
            rule.setTaxRuleCode(req.taxRuleCode());
            rule.setPriority(req.priority() != null ? req.priority() : 0);
            ruleRepository.save(rule);
        }
    }

    private void saveItemRules(FiscalOperation operation, List<FiscalOperationItemRuleRequest> itemRules) {
        if (itemRules == null) {
            return;
        }
        for (FiscalOperationItemRuleRequest req : itemRules) {
            FiscalOperationItemRule rule = new FiscalOperationItemRule();
            rule.setOperation(operation);
            rule.setNcmPrefix(req.ncmPrefix());
            rule.setProductId(req.productId());
            rule.setCfopOverride(req.cfopOverride());
            rule.setTaxRuleCode(req.taxRuleCode());
            itemRuleRepository.save(rule);
        }
    }

    private void saveStoreAssignments(FiscalOperation operation, List<UUID> storeIds, UUID organizationId) {
        if (storeIds == null) {
            return;
        }
        for (UUID storeId : storeIds) {
            Store store = storeService.requireUsable(storeId);
            if (!store.getOrganization().getId().equals(organizationId)) {
                throw new BusinessRuleException("Loja não pertence à organização");
            }
            FiscalOperationStoreAssignment assignment = new FiscalOperationStoreAssignment();
            assignment.setOperation(operation);
            assignment.setStore(store);
            storeAssignmentRepository.save(assignment);
        }
    }

    private FiscalOperationResponse toResponse(FiscalOperation operation) {
        List<FiscalOperationRuleResponse> rules =
                ruleRepository.findByOperationIdOrderByPriorityDesc(operation.getId()).stream()
                        .filter(r -> Boolean.TRUE.equals(r.getActive()))
                        .map(r -> new FiscalOperationRuleResponse(
                                r.getId(),
                                r.getOriginUf(),
                                r.getDestUf(),
                                r.getTaxpayerIndicator(),
                                r.getFinalConsumer(),
                                r.getCfop(),
                                r.getTaxRuleCode(),
                                r.getPriority()))
                        .toList();
        List<FiscalOperationItemRuleResponse> itemRules =
                itemRuleRepository.findByOperationId(operation.getId()).stream()
                        .filter(r -> Boolean.TRUE.equals(r.getActive()))
                        .map(r -> new FiscalOperationItemRuleResponse(
                                r.getId(), r.getNcmPrefix(), r.getProductId(), r.getCfopOverride(), r.getTaxRuleCode()))
                        .toList();
        List<UUID> storeIds = storeAssignmentRepository.findByOperationId(operation.getId()).stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .map(a -> a.getStore().getId())
                .toList();
        return new FiscalOperationResponse(
                operation.getId(),
                operation.getOrganization().getId(),
                operation.getCode(),
                operation.getName(),
                operation.getNatureOfOperation(),
                operation.getPurpose(),
                operation.getAllowedModels(),
                operation.getDefaultCfop(),
                operation.getGeneratesFinance(),
                operation.getMovesStock(),
                operation.getStockDirection(),
                operation.getRequiresReferencedDocument(),
                operation.getAllowsFinalConsumer(),
                operation.getValidFrom(),
                operation.getValidUntil(),
                operation.getStatus(),
                rules,
                itemRules,
                storeIds,
                operation.getCreatedAt());
    }

    private Map<String, Object> snapshot(FiscalOperation operation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", operation.getId());
        map.put("code", operation.getCode());
        map.put("name", operation.getName());
        return map;
    }

    private String normalizeUf(String uf) {
        return StringUtils.hasText(uf) ? uf.trim().toUpperCase() : null;
    }
}
