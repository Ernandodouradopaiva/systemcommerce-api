package br.com.systemcommerce.finance.costcenter.service;

import br.com.systemcommerce.finance.costcenter.dto.CostCenterAssignStoreRequest;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterCreateRequest;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterResponse;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterUpdateRequest;
import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import br.com.systemcommerce.finance.costcenter.entity.CostCenterHierarchy;
import br.com.systemcommerce.finance.costcenter.entity.CostCenterStoreAssignment;
import br.com.systemcommerce.finance.costcenter.repository.CostCenterHierarchyRepository;
import br.com.systemcommerce.finance.costcenter.repository.CostCenterRepository;
import br.com.systemcommerce.finance.costcenter.repository.CostCenterStoreAssignmentRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.AuditLogRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
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
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final CostCenterHierarchyRepository hierarchyRepository;
    private final CostCenterStoreAssignmentRepository assignmentRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<CostCenterResponse> list(
            UUID organizationId, UUID storeId, CostCenter.CostCenterStatus status, String search, Pageable pageable) {
        Specification<CostCenter> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (storeId != null) {
                preds.add(cb.or(cb.isNull(root.get("store")), cb.equal(root.get("store").get("id"), storeId)));
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
        return costCenterRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CostCenterResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CostCenterResponse> tree(UUID organizationId) {
        List<CostCenter> all = costCenterRepository.findByOrganizationIdOrderBySortOrderAscCodeAsc(organizationId);
        Map<UUID, List<CostCenter>> byParent = new HashMap<>();
        for (CostCenter c : all) {
            UUID pid = c.getParent() != null ? c.getParent().getId() : null;
            byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }
        return buildTree(null, byParent);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> history(UUID id) {
        getEntity(id);
        return auditLogRepository.findAll((root, query, cb) -> {
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(cb.equal(root.get("entityName"), "CostCenter"), cb.equal(root.get("entityId"), id));
        });
    }

    @Transactional
    public CostCenterResponse create(CostCenterCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        assertUnique(organization.getId(), request.code(), null);

        CostCenter parent = null;
        if (request.parentId() != null) {
            parent = getEntity(request.parentId());
            if (!parent.getOrganization().getId().equals(organization.getId())) {
                throw new BusinessRuleException("Centro pai deve pertencer à mesma organização");
            }
        }

        CostCenter center = new CostCenter();
        center.setOrganization(organization);
        center.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        center.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        center.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        center.setParent(parent);
        if (request.storeId() != null) {
            Store store = storeService.requireUsable(request.storeId());
            center.setStore(store);
        }
        if (request.responsibleUserId() != null) {
            User user = userRepository
                    .findById(request.responsibleUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            center.setResponsibleUser(user);
        }
        center.setAcceptsPosting(request.acceptsPosting() == null || request.acceptsPosting());
        center.setValidFrom(request.validFrom());
        center.setValidUntil(request.validUntil());
        center.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        center.setStatus(CostCenter.CostCenterStatus.ACTIVE);

        CostCenter saved = costCenterRepository.save(center);
        rebuildHierarchy(saved);
        domainAuditService.record(
                "FINANCE", "CostCenter", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Centro de custo criado");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public CostCenterResponse update(UUID id, CostCenterUpdateRequest request) {
        CostCenter center = getEntity(id);
        Map<String, Object> before = snapshot(center);
        assertUnique(center.getOrganization().getId(), request.code(), id);

        center.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        center.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        center.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        center.setStore(request.storeId() != null ? storeService.requireUsable(request.storeId()) : null);
        if (request.responsibleUserId() != null) {
            center.setResponsibleUser(userRepository
                    .findById(request.responsibleUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado")));
        } else {
            center.setResponsibleUser(null);
        }
        if (request.acceptsPosting() != null) {
            center.setAcceptsPosting(request.acceptsPosting());
        }
        center.setValidFrom(request.validFrom());
        center.setValidUntil(request.validUntil());
        if (request.sortOrder() != null) {
            center.setSortOrder(request.sortOrder());
        }
        costCenterRepository.save(center);
        domainAuditService.record(
                "FINANCE", "CostCenter", id, AuditLog.AuditAction.UPDATE, before, snapshot(center), "Centro de custo atualizado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public CostCenterResponse assignStore(UUID id, CostCenterAssignStoreRequest request) {
        CostCenter center = getEntity(id);
        Store store = storeService.requireUsable(request.storeId());
        if (assignmentRepository.existsByCostCenterIdAndStoreId(id, store.getId())) {
            throw new ConflictException("Loja já vinculada a este centro de custo");
        }
        CostCenterStoreAssignment assignment = new CostCenterStoreAssignment();
        assignment.setCostCenter(center);
        assignment.setStore(store);
        assignment.setPrimaryAssignment(Boolean.TRUE.equals(request.primaryAssignment()));
        assignment.setStatus(CostCenterStoreAssignment.AssignmentStatus.ACTIVE);
        assignmentRepository.save(assignment);
        domainAuditService.record(
                "FINANCE",
                "CostCenter",
                id,
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("storeId", store.getId()),
                "Loja vinculada ao centro de custo");
        return toResponse(getEntity(id));
    }

    @Transactional
    public CostCenterResponse activate(UUID id) {
        CostCenter center = getEntity(id);
        Map<String, Object> before = snapshot(center);
        center.markActive();
        costCenterRepository.save(center);
        domainAuditService.record(
                "FINANCE", "CostCenter", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(center), "Centro ativado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public CostCenterResponse inactivate(UUID id) {
        CostCenter center = getEntity(id);
        Map<String, Object> before = snapshot(center);
        center.markInactive();
        costCenterRepository.save(center);
        domainAuditService.record(
                "FINANCE",
                "CostCenter",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(center),
                "Centro inativado — histórico preservado");
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public CostCenter requirePostable(UUID id) {
        CostCenter center = getEntity(id);
        if (!center.isUsable() || !Boolean.TRUE.equals(center.getAcceptsPosting())) {
            throw new BusinessRuleException("Centro de custo não disponível para lançamento");
        }
        return center;
    }

    public CostCenter getEntity(UUID id) {
        return costCenterRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de custo não encontrado"));
    }

    private void rebuildHierarchy(CostCenter center) {
        if (hierarchyRepository.findByDescendantId(center.getId()).stream().noneMatch(h -> h.getDepth() == 0)) {
            CostCenterHierarchy self = new CostCenterHierarchy();
            self.setOrganizationId(center.getOrganization().getId());
            self.setAncestorId(center.getId());
            self.setDescendantId(center.getId());
            self.setDepth(0);
            hierarchyRepository.save(self);
        }
        if (center.getParent() != null) {
            if (hierarchyRepository.existsByAncestorIdAndDescendantId(center.getId(), center.getParent().getId())) {
                throw new BusinessRuleException("Hierarquia de centro de custo criaria ciclo");
            }
            for (CostCenterHierarchy pa : hierarchyRepository.findByDescendantId(center.getParent().getId())) {
                if (hierarchyRepository.existsByAncestorIdAndDescendantId(pa.getAncestorId(), center.getId())) {
                    continue;
                }
                CostCenterHierarchy link = new CostCenterHierarchy();
                link.setOrganizationId(center.getOrganization().getId());
                link.setAncestorId(pa.getAncestorId());
                link.setDescendantId(center.getId());
                link.setDepth(pa.getDepth() + 1);
                hierarchyRepository.save(link);
            }
        }
    }

    private List<CostCenterResponse> buildTree(UUID parentId, Map<UUID, List<CostCenter>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(CostCenter::getSortOrder).thenComparing(CostCenter::getCode))
                .map(c -> toResponse(c, buildTree(c.getId(), byParent)))
                .toList();
    }

    private CostCenterResponse toResponse(CostCenter c) {
        return toResponse(c, List.of());
    }

    private CostCenterResponse toResponse(CostCenter c, List<CostCenterResponse> children) {
        return new CostCenterResponse(
                c.getId(),
                c.getOrganization().getId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getStore() != null ? c.getStore().getId() : null,
                c.getStore() != null ? c.getStore().getCode() : null,
                c.getResponsibleUser() != null ? c.getResponsibleUser().getId() : null,
                Boolean.TRUE.equals(c.getAcceptsPosting()),
                c.getValidFrom(),
                c.getValidUntil(),
                c.getStatus(),
                c.isUsable(),
                c.getSortOrder(),
                c.getVersion(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                children);
    }

    private void assertUnique(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? costCenterRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : costCenterRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Já existe centro de custo com este código na organização");
        }
    }

    private Map<String, Object> snapshot(CostCenter c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("storeId", c.getStore() != null ? c.getStore().getId() : null);
        m.put("status", c.getStatus());
        m.put("acceptsPosting", c.getAcceptsPosting());
        return m;
    }
}
