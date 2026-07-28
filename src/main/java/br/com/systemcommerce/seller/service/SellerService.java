package br.com.systemcommerce.seller.service;

import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import br.com.systemcommerce.employee.repository.EmployeeStoreAssignmentRepository;
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.seller.dto.SellerDiscountLimitResponse;
import br.com.systemcommerce.seller.dto.SellerEnableRequest;
import br.com.systemcommerce.seller.dto.SellerResponse;
import br.com.systemcommerce.seller.dto.SellerStoreAssignmentResponse;
import br.com.systemcommerce.seller.dto.SellerStoreAuthorizeRequest;
import br.com.systemcommerce.seller.dto.SellerUpdateRequest;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.entity.SellerStoreAssignment;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.seller.repository.SellerStoreAssignmentRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final SellerStoreAssignmentRepository sellerStoreAssignmentRepository;
    private final EmployeeService employeeService;
    private final EmployeeStoreAssignmentRepository employeeStoreAssignmentRepository;
    private final StoreService storeService;
    private final SaleRepository saleRepository;
    private final DomainAuditService domainAuditService;

    @Value("${app.seller.lotation-grants-commercial-auth:false}")
    private boolean lotationGrantsCommercialAuth;

    @Transactional
    public SellerResponse enable(SellerEnableRequest request) {
        Employee employee = employeeService.getEntity(request.employeeId());
        if (!employee.isOperationallyActive()) {
            throw new BusinessRuleException("Vendedor deve estar vinculado a um profissional ativo");
        }
        if (sellerProfileRepository.existsByEmployeeId(employee.getId())) {
            throw new ConflictException("Profissional já possui perfil de vendedor");
        }
        assertUniqueCode(employee.getOrganization().getId(), request.sellerCode(), null);
        SellerProfile profile = new SellerProfile();
        profile.setOrganization(employee.getOrganization());
        profile.setEmployee(employee);
        profile.setSellerCode(MoneyAndQuantityUtils.requireText(request.sellerCode(), "Código").toUpperCase());
        profile.setMaxDiscountPercent(normalizeDiscount(request.maxDiscountPercent()));
        profile.setAllowsExternalSale(Boolean.TRUE.equals(request.allowsExternalSale()));
        profile.setAllowsOtherStores(Boolean.TRUE.equals(request.allowsOtherStores()));
        profile.setMonthlyTargetAmount(request.monthlyTargetAmount());
        profile.setDefaultCommissionPercent(normalizeDiscount(request.defaultCommissionPercent()));
        if (request.supervisorEmployeeId() != null) {
            profile.setSupervisor(employeeService.getEntity(request.supervisorEmployeeId()));
        }
        profile.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        profile.enable();
        employee.setCanSell(true);
        SellerProfile saved = sellerProfileRepository.save(profile);
        domainAuditService.record(
                "SELLER",
                "SellerProfile",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Profissional habilitado como vendedor");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public SellerResponse disable(UUID id) {
        SellerProfile profile = getEntity(id);
        Map<String, Object> before = snapshot(profile);
        profile.disable();
        profile.getEmployee().setCanSell(false);
        sellerProfileRepository.save(profile);
        domainAuditService.record(
                "SELLER",
                "SellerProfile",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(profile),
                "Vendedor desabilitado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public SellerResponse update(UUID id, SellerUpdateRequest request) {
        SellerProfile profile = getEntity(id);
        Map<String, Object> before = snapshot(profile);
        assertUniqueCode(profile.getOrganization().getId(), request.sellerCode(), id);
        profile.setSellerCode(MoneyAndQuantityUtils.requireText(request.sellerCode(), "Código").toUpperCase());
        if (request.maxDiscountPercent() != null) {
            profile.setMaxDiscountPercent(normalizeDiscount(request.maxDiscountPercent()));
        }
        if (request.allowsExternalSale() != null) {
            profile.setAllowsExternalSale(request.allowsExternalSale());
        }
        if (request.allowsOtherStores() != null) {
            profile.setAllowsOtherStores(request.allowsOtherStores());
        }
        profile.setMonthlyTargetAmount(request.monthlyTargetAmount());
        if (request.defaultCommissionPercent() != null) {
            profile.setDefaultCommissionPercent(normalizeDiscount(request.defaultCommissionPercent()));
        }
        if (request.supervisorEmployeeId() != null) {
            profile.setSupervisor(employeeService.getEntity(request.supervisorEmployeeId()));
        } else {
            profile.setSupervisor(null);
        }
        profile.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        sellerProfileRepository.save(profile);
        domainAuditService.record(
                "SELLER",
                "SellerProfile",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(profile),
                "Vendedor atualizado");
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public SellerResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<SellerResponse> list(UUID organizationId, SellerProfile.SellerStatus status, String search, Pageable pageable) {
        Specification<SellerProfile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sellerCode")), pattern),
                        cb.like(cb.lower(root.get("employee").get("name")), pattern),
                        cb.like(cb.lower(root.get("employee").get("registrationNumber")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return sellerProfileRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<SellerResponse> listByStore(UUID storeId) {
        return sellerStoreAssignmentRepository.findActiveSellersByStore(storeId, LocalDate.now()).stream()
                .map(a -> toResponse(a.getSellerProfile()))
                .distinct()
                .toList();
    }

    @Transactional
    public SellerStoreAssignmentResponse authorizeStore(UUID sellerId, SellerStoreAuthorizeRequest request) {
        SellerProfile profile = getEntity(sellerId);
        if (!profile.isEnabledForSales()) {
            throw new BusinessRuleException("Vendedor inativo ou profissional desligado não pode receber autorização");
        }
        Store store = storeService.getEntity(request.storeId());
        if (!store.getOrganization().getId().equals(profile.getOrganization().getId())) {
            throw new BusinessRuleException("Loja deve pertencer à mesma organização do vendedor");
        }
        boolean temporary = Boolean.TRUE.equals(request.temporary());
        if (temporary && request.endDate() == null) {
            throw new BusinessRuleException("Autorização temporária deve possuir período (data de término)");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("Período de autorização inconsistente");
        }
        if (!lotationGrantsCommercialAuth) {
            boolean hasLotation = employeeStoreAssignmentRepository
                    .findActiveOnDate(profile.getEmployee().getId(), LocalDate.now())
                    .stream()
                    .anyMatch(a -> a.getStore().getId().equals(store.getId()));
            if (!hasLotation
                    && !profile.isAllowsOtherStores()
                    && !SecurityAuthorities.hasAuthority("SELLER_AUTHORIZE_OTHER_STORE")) {
                throw new BusinessRuleException(
                        "Lotação profissional não concede autorização comercial automaticamente; "
                                + "exige SELLER_AUTHORIZE_OTHER_STORE ou allowsOtherStores");
            }
        }
        boolean primary = Boolean.TRUE.equals(request.primary());
        if (primary) {
            sellerStoreAssignmentRepository.findHistoryBySellerId(sellerId).stream()
                    .filter(a -> a.getStatus() == SellerStoreAssignment.AssignmentStatus.ACTIVE
                            && a.isPrimaryAssignment())
                    .forEach(a -> {
                        a.setPrimaryAssignment(false);
                        sellerStoreAssignmentRepository.save(a);
                    });
        }
        SellerStoreAssignment assignment = new SellerStoreAssignment();
        assignment.setSellerProfile(profile);
        assignment.setStore(store);
        assignment.setStartDate(request.startDate());
        assignment.setEndDate(request.endDate());
        assignment.setPrimaryAssignment(primary);
        assignment.setTemporaryAssignment(temporary);
        assignment.setAllowsRegisterSale(request.allowsRegisterSale() == null || request.allowsRegisterSale());
        assignment.setMaxDiscountPercent(
                request.maxDiscountPercent() != null ? normalizeDiscount(request.maxDiscountPercent()) : null);
        assignment.setTargetAmount(request.targetAmount());
        assignment.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        assignment.setStatus(SellerStoreAssignment.AssignmentStatus.ACTIVE);
        assignment.setActive(true);
        SellerStoreAssignment saved = sellerStoreAssignmentRepository.save(assignment);
        domainAuditService.record(
                "SELLER",
                "SellerStoreAssignment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of(
                        "sellerId",
                        sellerId,
                        "storeId",
                        store.getId(),
                        "temporary",
                        temporary),
                temporary ? "Autorização comercial temporária concedida" : "Autorização comercial concedida");
        return toAssignmentResponse(getAssignment(saved.getId()));
    }

    @Transactional
    public SellerStoreAssignmentResponse revokeStoreAuthorization(UUID sellerId, UUID assignmentId) {
        SellerStoreAssignment assignment = getAssignmentForSeller(sellerId, assignmentId);
        Map<String, Object> before = Map.of("status", assignment.getStatus(), "storeId", assignment.getStore().getId());
        assignment.revoke();
        sellerStoreAssignmentRepository.save(assignment);
        domainAuditService.record(
                "SELLER",
                "SellerStoreAssignment",
                assignmentId,
                AuditLog.AuditAction.UPDATE,
                before,
                Map.of("status", assignment.getStatus()),
                "Autorização comercial removida/revogada");
        return toAssignmentResponse(getAssignment(assignmentId));
    }

    @Transactional(readOnly = true)
    public List<SellerStoreAssignmentResponse> listAllowedStores(UUID sellerId) {
        getEntity(sellerId);
        return sellerStoreAssignmentRepository.findHistoryBySellerId(sellerId).stream()
                .filter(a -> a.isEffectiveOn(LocalDate.now()))
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SellerStoreAssignmentResponse> listHistory(UUID sellerId) {
        getEntity(sellerId);
        return sellerStoreAssignmentRepository.findHistoryBySellerId(sellerId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerDiscountLimitResponse getDiscountLimit(UUID sellerId, UUID storeId) {
        SellerProfile profile = getEntity(sellerId);
        BigDecimal profileLimit = profile.getMaxDiscountPercent();
        BigDecimal storeLimit = null;
        if (storeId != null) {
            storeLimit = sellerStoreAssignmentRepository
                    .findEffectiveAuthorization(sellerId, storeId, LocalDate.now())
                    .map(SellerStoreAssignment::getMaxDiscountPercent)
                    .orElse(null);
        }
        BigDecimal effective = profileLimit;
        if (storeLimit != null) {
            effective = storeLimit.min(profileLimit);
        }
        return new SellerDiscountLimitResponse(sellerId, storeId, profileLimit, storeLimit, effective);
    }

    @Transactional(readOnly = true)
    public Page<Sale> listSales(UUID sellerId, Pageable pageable) {
        getEntity(sellerId);
        return saleRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("sellerProfile").get("id"), sellerId), pageable);
    }

    /**
     * Valida se o vendedor pode ser informado em nova venda na loja.
     * Lotação RH sozinha não autoriza (salvo configuração).
     */
    @Transactional(readOnly = true)
    public SellerProfile requireAuthorizedForSale(UUID sellerProfileId, UUID storeId) {
        SellerProfile profile = getEntity(sellerProfileId);
        if (!profile.isEnabledForSales()) {
            throw new BusinessRuleException("Vendedor inativo ou profissional desligado não pode ser informado na venda");
        }
        if (storeId == null) {
            throw new BusinessRuleException("Venda com vendedor exige loja");
        }
        var auth = sellerStoreAssignmentRepository.findEffectiveAuthorization(sellerProfileId, storeId, LocalDate.now());
        if (auth.isPresent()) {
            return profile;
        }
        if (lotationGrantsCommercialAuth) {
            boolean lotation = employeeStoreAssignmentRepository
                    .findActiveOnDate(profile.getEmployee().getId(), LocalDate.now())
                    .stream()
                    .anyMatch(a -> a.getStore().getId().equals(storeId)
                            && a.getStatus() == EmployeeStoreAssignment.AssignmentStatus.ACTIVE);
            if (lotation) {
                return profile;
            }
        }
        throw new BusinessRuleException("Vendedor não autorizado a vender nesta loja");
    }

    @Transactional(readOnly = true)
    public SellerProfile getEntity(UUID id) {
        return sellerProfileRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", id));
    }

    private SellerStoreAssignment getAssignment(UUID id) {
        return sellerStoreAssignmentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autorização de vendedor", id));
    }

    private SellerStoreAssignment getAssignmentForSeller(UUID sellerId, UUID assignmentId) {
        SellerStoreAssignment assignment = getAssignment(assignmentId);
        if (!assignment.getSellerProfile().getId().equals(sellerId)) {
            throw new ResourceNotFoundException("Autorização de vendedor", assignmentId);
        }
        return assignment;
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? sellerProfileRepository.existsByOrganizationIdAndSellerCodeIgnoreCase(organizationId, normalized)
                : sellerProfileRepository.existsByOrganizationIdAndSellerCodeIgnoreCaseAndIdNot(
                        organizationId, normalized, id);
        if (exists) {
            throw new ConflictException("Código de vendedor já está em uso nesta organização");
        }
    }

    private BigDecimal normalizeDiscount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRuleException("Percentual máximo de desconto deve estar entre 0 e 100");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private SellerResponse toResponse(SellerProfile profile) {
        return new SellerResponse(
                profile.getId(),
                profile.getOrganization().getId(),
                profile.getEmployee().getId(),
                profile.getEmployee().getName(),
                profile.getEmployee().getRegistrationNumber(),
                profile.getSellerCode(),
                profile.getStatus(),
                profile.getMaxDiscountPercent(),
                profile.isAllowsExternalSale(),
                profile.isAllowsOtherStores(),
                profile.getMonthlyTargetAmount(),
                profile.getDefaultCommissionPercent(),
                profile.getSupervisor() != null ? profile.getSupervisor().getId() : null,
                profile.getEnabledAt(),
                profile.getDisabledAt(),
                profile.getNotes(),
                profile.getActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private SellerStoreAssignmentResponse toAssignmentResponse(SellerStoreAssignment assignment) {
        return new SellerStoreAssignmentResponse(
                assignment.getId(),
                assignment.getSellerProfile().getId(),
                assignment.getStore().getId(),
                assignment.getStore().getCode(),
                assignment.getStore().getName(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.isPrimaryAssignment(),
                assignment.isTemporaryAssignment(),
                assignment.isAllowsRegisterSale(),
                assignment.getMaxDiscountPercent(),
                assignment.getTargetAmount(),
                assignment.getStatus(),
                assignment.getNotes(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    private Map<String, Object> snapshot(SellerProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", profile.getId());
        map.put("sellerCode", profile.getSellerCode());
        map.put("status", profile.getStatus());
        map.put("maxDiscountPercent", profile.getMaxDiscountPercent());
        map.put("defaultCommissionPercent", profile.getDefaultCommissionPercent());
        map.put("employeeId", profile.getEmployee() != null ? profile.getEmployee().getId() : null);
        return map;
    }
}
