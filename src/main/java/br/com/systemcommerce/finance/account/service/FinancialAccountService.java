package br.com.systemcommerce.finance.account.service;

import br.com.systemcommerce.finance.account.dto.FinancialAccountCreateRequest;
import br.com.systemcommerce.finance.account.dto.FinancialAccountReorganizeRequest;
import br.com.systemcommerce.finance.account.dto.FinancialAccountResponse;
import br.com.systemcommerce.finance.account.dto.FinancialAccountUpdateRequest;
import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.entity.FinancialAccountHierarchy;
import br.com.systemcommerce.finance.account.mapper.FinancialAccountMapper;
import br.com.systemcommerce.finance.account.repository.FinancialAccountHierarchyRepository;
import br.com.systemcommerce.finance.account.repository.FinancialAccountRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.AuditLogRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
public class FinancialAccountService {

    private final FinancialAccountRepository accountRepository;
    private final FinancialAccountHierarchyRepository hierarchyRepository;
    private final FinancialAccountMapper mapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<FinancialAccountResponse> list(
            UUID organizationId, FinancialAccount.AccountStatus status, String search, Pageable pageable) {
        Specification<FinancialAccount> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
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
        return accountRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FinancialAccountResponse getById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<FinancialAccountResponse> tree(UUID organizationId) {
        List<FinancialAccount> all = accountRepository.findByOrganizationIdOrderBySortOrderAscCodeAsc(organizationId);
        Map<UUID, List<FinancialAccount>> byParent = new HashMap<>();
        for (FinancialAccount a : all) {
            UUID pid = a.getParent() != null ? a.getParent().getId() : null;
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(a);
        }
        return buildTree(null, byParent);
    }

    @Transactional(readOnly = true)
    public List<FinancialAccountResponse> postable(UUID organizationId) {
        return accountRepository
                .findByOrganizationIdAndAcceptsPostingTrueAndStatusOrderByCodeAsc(
                        organizationId, FinancialAccount.AccountStatus.ACTIVE)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> history(UUID id) {
        getEntity(id);
        return auditLogRepository.findAll((root, query, cb) -> {
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(
                    cb.equal(root.get("entityName"), "FinancialAccount"),
                    cb.equal(root.get("entityId"), id));
        });
    }

    @Transactional
    public FinancialAccountResponse create(FinancialAccountCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        assertUniqueCode(organization.getId(), request.code(), null);

        FinancialAccount parent = null;
        int level = 1;
        if (request.parentId() != null) {
            parent = getEntity(request.parentId());
            if (!parent.getOrganization().getId().equals(organization.getId())) {
                throw new BusinessRuleException("Conta pai deve pertencer à mesma organização");
            }
            if (Boolean.TRUE.equals(parent.getAcceptsPosting())) {
                throw new BusinessRuleException("Conta analítica não pode ter filhos — use conta sintética como pai");
            }
            level = parent.getLevelNo() + 1;
        }

        FinancialAccount account = new FinancialAccount();
        account.setOrganization(organization);
        account.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        account.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        account.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        account.setParent(parent);
        account.setLevelNo(level);
        account.setAccountType(request.accountType());
        account.setNature(request.nature());
        account.setAcceptsPosting(Boolean.TRUE.equals(request.acceptsPosting()));
        account.setRequiresCostCenter(Boolean.TRUE.equals(request.requiresCostCenter()));
        account.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        account.setStatus(FinancialAccount.AccountStatus.ACTIVE);

        FinancialAccount saved = accountRepository.save(account);
        rebuildHierarchyForNode(saved);
        domainAuditService.record(
                "FINANCE", "FinancialAccount", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Conta financeira criada");
        return mapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public FinancialAccountResponse update(UUID id, FinancialAccountUpdateRequest request) {
        FinancialAccount account = getEntity(id);
        Map<String, Object> before = snapshot(account);
        assertUniqueCode(account.getOrganization().getId(), request.code(), id);

        if (request.nature() != account.getNature()
                && hierarchyRepository.countByAncestorIdAndDepthGreaterThan(id, 0) > 0) {
            throw new BusinessRuleException("Mudança de natureza exige conta sem descendentes");
        }
        if (Boolean.TRUE.equals(request.acceptsPosting()) && accountRepository.countByParentId(id) > 0) {
            throw new BusinessRuleException("Conta com filhos deve permanecer sintética (não aceita lançamento)");
        }

        account.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        account.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        account.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        account.setAccountType(request.accountType());
        account.setNature(request.nature());
        account.setAcceptsPosting(Boolean.TRUE.equals(request.acceptsPosting()));
        account.setRequiresCostCenter(Boolean.TRUE.equals(request.requiresCostCenter()));
        if (request.sortOrder() != null) {
            account.setSortOrder(request.sortOrder());
        }

        FinancialAccount saved = accountRepository.save(account);
        domainAuditService.record(
                "FINANCE", "FinancialAccount", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Conta financeira atualizada");
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public FinancialAccountResponse reorganize(UUID id, FinancialAccountReorganizeRequest request) {
        FinancialAccount account = getEntity(id);
        Map<String, Object> before = snapshot(account);

        FinancialAccount newParent = null;
        int level = 1;
        if (request.newParentId() != null) {
            if (request.newParentId().equals(id)) {
                throw new BusinessRuleException("Conta não pode ser pai de si mesma");
            }
            if (hierarchyRepository.existsByAncestorIdAndDescendantId(id, request.newParentId())) {
                throw new BusinessRuleException("Reorganização criaria ciclo hierárquico");
            }
            newParent = getEntity(request.newParentId());
            if (!newParent.getOrganization().getId().equals(account.getOrganization().getId())) {
                throw new BusinessRuleException("Conta pai deve pertencer à mesma organização");
            }
            if (Boolean.TRUE.equals(newParent.getAcceptsPosting())) {
                throw new BusinessRuleException("Conta analítica não pode ser pai");
            }
            level = newParent.getLevelNo() + 1;
        }

        hierarchyRepository.deleteCrossLinksForSubtree(id);
        hierarchyRepository.deleteNonSelfByDescendantId(id);

        account.setParent(newParent);
        account.setLevelNo(level);
        if (request.sortOrder() != null) {
            account.setSortOrder(request.sortOrder());
        }
        FinancialAccount saved = accountRepository.save(account);
        rebuildHierarchyForNode(saved);
        recountLevels(saved);

        domainAuditService.record(
                "FINANCE",
                "FinancialAccount",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Conta reorganizada na hierarquia");
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public FinancialAccountResponse activate(UUID id) {
        FinancialAccount account = getEntity(id);
        Map<String, Object> before = snapshot(account);
        account.markActive();
        accountRepository.save(account);
        domainAuditService.record(
                "FINANCE", "FinancialAccount", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(account), "Conta ativada");
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public FinancialAccountResponse inactivate(UUID id) {
        FinancialAccount account = getEntity(id);
        Map<String, Object> before = snapshot(account);
        account.markInactive();
        accountRepository.save(account);
        domainAuditService.record(
                "FINANCE",
                "FinancialAccount",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(account),
                "Conta inativada — históricos preservados");
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public FinancialAccount requirePostable(UUID id) {
        FinancialAccount account = getEntity(id);
        if (!account.isUsable() || !Boolean.TRUE.equals(account.getAcceptsPosting())) {
            throw new BusinessRuleException("Conta financeira não está disponível para lançamento");
        }
        return account;
    }

    @Transactional(readOnly = true)
    public FinancialAccount getEntity(UUID id) {
        return accountRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta financeira não encontrada"));
    }

    private void rebuildHierarchyForNode(FinancialAccount account) {
        if (hierarchyRepository.findByDescendantId(account.getId()).stream().noneMatch(h -> h.getDepth() == 0)) {
            FinancialAccountHierarchy self = new FinancialAccountHierarchy();
            self.setOrganizationId(account.getOrganization().getId());
            self.setAncestorId(account.getId());
            self.setDescendantId(account.getId());
            self.setDepth(0);
            hierarchyRepository.save(self);
        }
        if (account.getParent() != null) {
            List<FinancialAccountHierarchy> parentAncestors =
                    hierarchyRepository.findByDescendantId(account.getParent().getId());
            for (FinancialAccountHierarchy pa : parentAncestors) {
                if (hierarchyRepository.existsByAncestorIdAndDescendantId(pa.getAncestorId(), account.getId())) {
                    continue;
                }
                FinancialAccountHierarchy link = new FinancialAccountHierarchy();
                link.setOrganizationId(account.getOrganization().getId());
                link.setAncestorId(pa.getAncestorId());
                link.setDescendantId(account.getId());
                link.setDepth(pa.getDepth() + 1);
                hierarchyRepository.save(link);
            }
        }
    }

    private void recountLevels(FinancialAccount root) {
        List<FinancialAccountHierarchy> descendants = hierarchyRepository.findByAncestorId(root.getId());
        for (FinancialAccountHierarchy h : descendants) {
            FinancialAccount node = accountRepository.findById(h.getDescendantId()).orElse(null);
            if (node == null) {
                continue;
            }
            int level = 1;
            FinancialAccount p = node.getParent();
            while (p != null) {
                level++;
                p = p.getParent();
            }
            node.setLevelNo(level);
            accountRepository.save(node);
        }
    }

    private List<FinancialAccountResponse> buildTree(UUID parentId, Map<UUID, List<FinancialAccount>> byParent) {
        List<FinancialAccount> nodes = byParent.getOrDefault(parentId, List.of());
        return nodes.stream()
                .sorted(Comparator.comparing(FinancialAccount::getSortOrder).thenComparing(FinancialAccount::getCode))
                .map(n -> mapper.toResponse(n, buildTree(n.getId(), byParent)))
                .toList();
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? accountRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : accountRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Já existe conta financeira com este código na organização");
        }
    }

    private Map<String, Object> snapshot(FinancialAccount a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", a.getCode());
        m.put("name", a.getName());
        m.put("parentId", a.getParent() != null ? a.getParent().getId() : null);
        m.put("levelNo", a.getLevelNo());
        m.put("accountType", a.getAccountType());
        m.put("nature", a.getNature());
        m.put("acceptsPosting", a.getAcceptsPosting());
        m.put("status", a.getStatus());
        m.put("sortOrder", a.getSortOrder());
        return m;
    }
}
