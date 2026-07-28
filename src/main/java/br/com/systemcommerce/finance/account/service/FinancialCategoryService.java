package br.com.systemcommerce.finance.account.service;

import br.com.systemcommerce.finance.account.dto.FinancialCategoryCreateRequest;
import br.com.systemcommerce.finance.account.dto.FinancialCategoryResponse;
import br.com.systemcommerce.finance.account.dto.FinancialCategoryUpdateRequest;
import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.account.mapper.FinancialAccountMapper;
import br.com.systemcommerce.finance.account.repository.FinancialCategoryRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FinancialCategoryService {

    private final FinancialCategoryRepository categoryRepository;
    private final FinancialAccountService accountService;
    private final FinancialAccountMapper mapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<FinancialCategoryResponse> list(
            UUID organizationId,
            FinancialCategory.UsageScope usageScope,
            FinancialCategory.CategoryStatus status,
            String search,
            Pageable pageable) {
        Specification<FinancialCategory> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (usageScope != null) {
                preds.add(cb.or(
                        cb.equal(root.get("usageScope"), usageScope),
                        cb.equal(root.get("usageScope"), FinancialCategory.UsageScope.BOTH)));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like)));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return categoryRepository.findAll(spec, pageable).map(mapper::toCategoryResponse);
    }

    @Transactional(readOnly = true)
    public FinancialCategoryResponse getById(UUID id) {
        return mapper.toCategoryResponse(getEntity(id));
    }

    @Transactional
    public FinancialCategoryResponse create(FinancialCategoryCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        assertUnique(organization.getId(), request.code(), null);

        FinancialCategory category = new FinancialCategory();
        category.setOrganization(organization);
        category.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        category.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        category.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        category.setUsageScope(request.usageScope());
        category.setStatus(FinancialCategory.CategoryStatus.ACTIVE);
        if (request.financialAccountId() != null) {
            FinancialAccount account = accountService.requirePostable(request.financialAccountId());
            if (!account.getOrganization().getId().equals(organization.getId())) {
                throw new BusinessRuleException("Conta financeira deve pertencer à mesma organização");
            }
            category.setFinancialAccount(account);
        }

        FinancialCategory saved = categoryRepository.save(category);
        domainAuditService.record(
                "FINANCE",
                "FinancialCategory",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Categoria financeira criada");
        return mapper.toCategoryResponse(getEntity(saved.getId()));
    }

    @Transactional
    public FinancialCategoryResponse update(UUID id, FinancialCategoryUpdateRequest request) {
        FinancialCategory category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        assertUnique(category.getOrganization().getId(), request.code(), id);

        category.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        category.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        category.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        category.setUsageScope(request.usageScope());
        if (request.financialAccountId() != null) {
            FinancialAccount account = accountService.requirePostable(request.financialAccountId());
            if (!account.getOrganization().getId().equals(category.getOrganization().getId())) {
                throw new BusinessRuleException("Conta financeira deve pertencer à mesma organização");
            }
            category.setFinancialAccount(account);
        } else {
            category.setFinancialAccount(null);
        }

        categoryRepository.save(category);
        domainAuditService.record(
                "FINANCE",
                "FinancialCategory",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(category),
                "Categoria financeira atualizada");
        return mapper.toCategoryResponse(getEntity(id));
    }

    @Transactional
    public FinancialCategoryResponse activate(UUID id) {
        FinancialCategory category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        category.markActive();
        categoryRepository.save(category);
        domainAuditService.record(
                "FINANCE", "FinancialCategory", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(category), "Categoria ativada");
        return mapper.toCategoryResponse(getEntity(id));
    }

    @Transactional
    public FinancialCategoryResponse inactivate(UUID id) {
        FinancialCategory category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        category.markInactive();
        categoryRepository.save(category);
        domainAuditService.record(
                "FINANCE",
                "FinancialCategory",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(category),
                "Categoria inativada");
        return mapper.toCategoryResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public FinancialCategory requireUsable(UUID id, FinancialCategory.UsageScope requiredScope) {
        FinancialCategory category = getEntity(id);
        if (!category.isUsable()) {
            throw new BusinessRuleException("Categoria financeira inativa não pode ser utilizada");
        }
        if (requiredScope != null
                && category.getUsageScope() != FinancialCategory.UsageScope.BOTH
                && category.getUsageScope() != requiredScope) {
            throw new BusinessRuleException("Categoria financeira não disponível para o escopo " + requiredScope);
        }
        return category;
    }

    private FinancialCategory getEntity(UUID id) {
        return categoryRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria financeira não encontrada"));
    }

    private void assertUnique(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? categoryRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : categoryRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Já existe categoria financeira com este código na organização");
        }
    }

    private Map<String, Object> snapshot(FinancialCategory c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("usageScope", c.getUsageScope());
        m.put("status", c.getStatus());
        m.put("financialAccountId", c.getFinancialAccount() != null ? c.getFinancialAccount().getId() : null);
        return m;
    }
}
