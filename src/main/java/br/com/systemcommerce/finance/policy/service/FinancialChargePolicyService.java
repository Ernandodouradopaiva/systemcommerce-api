package br.com.systemcommerce.finance.policy.service;

import br.com.systemcommerce.finance.policy.dto.FinancialChargePolicyDtos.*;
import br.com.systemcommerce.finance.policy.entity.FinancialChargePolicy;
import br.com.systemcommerce.finance.policy.repository.FinancialChargePolicyRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialChargePolicyService {

    private final FinancialChargePolicyRepository policyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<Response> list(UUID organizationId, Pageable pageable) {
        Specification<FinancialChargePolicy> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return policyRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public Response create(CreateRequest request) {
        assertUniqueCode(request.organizationId(), request.code(), null);
        FinancialChargePolicy policy = new FinancialChargePolicy();
        policy.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            policy.setStore(storeService.requireUsable(request.storeId()));
        }
        policy.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        applyFields(
                policy,
                request.name(),
                request.description(),
                request.priority(),
                request.validFrom(),
                request.validTo(),
                request.interestType(),
                request.interestRate(),
                request.interestGraceDays(),
                request.penaltyType(),
                request.penaltyFixedAmount(),
                request.penaltyPercent(),
                request.earlyDiscountType(),
                request.earlyDiscountPercent(),
                request.earlyDiscountDays(),
                request.maxAuthorizedDiscountPercent(),
                request.requiresDiscountAuthorization(),
                request.roundingMode());
        policy.setStatus(FinancialChargePolicy.Status.ACTIVE);
        FinancialChargePolicy saved = policyRepository.save(policy);
        domainAuditService.record(
                "FINANCE",
                "FinancialChargePolicy",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Política financeira criada");
        return toResponse(require(saved.getId()));
    }

    @Transactional
    public Response update(UUID id, UpdateRequest request) {
        FinancialChargePolicy policy = require(id);
        applyFields(
                policy,
                request.name(),
                request.description(),
                request.priority(),
                request.validFrom(),
                request.validTo(),
                request.interestType(),
                request.interestRate(),
                request.interestGraceDays(),
                request.penaltyType(),
                request.penaltyFixedAmount(),
                request.penaltyPercent(),
                request.earlyDiscountType(),
                request.earlyDiscountPercent(),
                request.earlyDiscountDays(),
                request.maxAuthorizedDiscountPercent(),
                request.requiresDiscountAuthorization(),
                request.roundingMode());
        if (request.status() != null) {
            policy.setStatus(request.status());
        }
        policyRepository.save(policy);
        domainAuditService.record(
                "FINANCE",
                "FinancialChargePolicy",
                id,
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Política financeira atualizada");
        return toResponse(require(id));
    }

    /**
     * Cálculo exclusivo da API — juros, multa e desconto nunca negativos; total nunca negativo.
     */
    @Transactional(readOnly = true)
    public SimulateResponse simulate(SimulateRequest request) {
        FinancialChargePolicy policy = require(request.policyId());
        RoundingMode rm = toRounding(policy.getRoundingMode());
        BigDecimal principal = nonNegative(request.principal()).setScale(2, rm);

        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        LocalDate dueDate = request.dueDate();
        LocalDate payDate = request.payDate();

        if (payDate.isAfter(dueDate)) {
            long overdueDays = ChronoUnit.DAYS.between(dueDate, payDate);
            int grace = policy.getInterestGraceDays() != null ? policy.getInterestGraceDays() : 0;
            long chargeableDays = Math.max(0, overdueDays - grace);

            if (policy.getInterestType() == FinancialChargePolicy.InterestType.SIMPLE_DAILY && chargeableDays > 0) {
                BigDecimal rate = nz(policy.getInterestRate());
                interest = principal
                        .multiply(rate)
                        .multiply(BigDecimal.valueOf(chargeableDays))
                        .setScale(2, rm);
            }

            if (policy.getPenaltyType() == FinancialChargePolicy.PenaltyType.FIXED) {
                penalty = nonNegative(nz(policy.getPenaltyFixedAmount())).setScale(2, rm);
            } else if (policy.getPenaltyType() == FinancialChargePolicy.PenaltyType.PERCENT) {
                penalty = principal
                        .multiply(nz(policy.getPenaltyPercent()))
                        .divide(BigDecimal.valueOf(100), 8, rm)
                        .setScale(2, rm);
            }
        } else if (payDate.isBefore(dueDate)
                && policy.getEarlyDiscountType() == FinancialChargePolicy.EarlyDiscountType.PERCENT) {
            int earlyDays = policy.getEarlyDiscountDays() != null ? policy.getEarlyDiscountDays() : 0;
            LocalDate cutoff = dueDate.minusDays(earlyDays);
            if (!payDate.isAfter(cutoff)) {
                discount = principal
                        .multiply(nz(policy.getEarlyDiscountPercent()))
                        .divide(BigDecimal.valueOf(100), 8, rm)
                        .setScale(2, rm);
            }
        }

        BigDecimal authorized = nonNegative(nz(request.authorizedDiscount())).setScale(2, rm);
        if (authorized.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxPct = nz(policy.getMaxAuthorizedDiscountPercent());
            if (maxPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxAmount = principal
                        .multiply(maxPct)
                        .divide(BigDecimal.valueOf(100), 8, rm)
                        .setScale(2, rm);
                if (authorized.compareTo(maxAmount) > 0) {
                    throw new BusinessRuleException(
                            "Desconto autorizado acima do máximo permitido pela política (" + maxAmount + ")");
                }
            }
            if (Boolean.TRUE.equals(policy.getRequiresDiscountAuthorization())
                    && authorized.compareTo(BigDecimal.ZERO) > 0
                    && request.authorizedDiscount() == null) {
                throw new BusinessRuleException("Política exige autorização de desconto");
            }
            discount = discount.add(authorized);
        }

        interest = nonNegative(interest);
        penalty = nonNegative(penalty);
        discount = nonNegative(discount);

        BigDecimal total = principal.add(interest).add(penalty).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO.setScale(2, rm);
        } else {
            total = total.setScale(2, rm);
        }

        return new SimulateResponse(
                principal, interest, penalty, discount, total, policy.getId(), policy.getCode());
    }

    private void applyFields(
            FinancialChargePolicy policy,
            String name,
            String description,
            Integer priority,
            LocalDate validFrom,
            LocalDate validTo,
            FinancialChargePolicy.InterestType interestType,
            BigDecimal interestRate,
            Integer interestGraceDays,
            FinancialChargePolicy.PenaltyType penaltyType,
            BigDecimal penaltyFixedAmount,
            BigDecimal penaltyPercent,
            FinancialChargePolicy.EarlyDiscountType earlyDiscountType,
            BigDecimal earlyDiscountPercent,
            Integer earlyDiscountDays,
            BigDecimal maxAuthorizedDiscountPercent,
            Boolean requiresDiscountAuthorization,
            FinancialChargePolicy.RoundingModeType roundingMode) {
        policy.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        policy.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        policy.setPriority(priority != null ? priority : 100);
        policy.setValidFrom(validFrom);
        policy.setValidTo(validTo);
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("validTo não pode ser anterior a validFrom");
        }
        policy.setInterestType(interestType != null ? interestType : FinancialChargePolicy.InterestType.NONE);
        policy.setInterestRate(nz(interestRate));
        policy.setInterestGraceDays(interestGraceDays != null ? interestGraceDays : 0);
        policy.setPenaltyType(penaltyType != null ? penaltyType : FinancialChargePolicy.PenaltyType.NONE);
        policy.setPenaltyFixedAmount(nz(penaltyFixedAmount));
        policy.setPenaltyPercent(nz(penaltyPercent));
        policy.setEarlyDiscountType(
                earlyDiscountType != null ? earlyDiscountType : FinancialChargePolicy.EarlyDiscountType.NONE);
        policy.setEarlyDiscountPercent(nz(earlyDiscountPercent));
        policy.setEarlyDiscountDays(earlyDiscountDays != null ? earlyDiscountDays : 0);
        policy.setMaxAuthorizedDiscountPercent(nz(maxAuthorizedDiscountPercent));
        policy.setRequiresDiscountAuthorization(
                requiresDiscountAuthorization != null ? requiresDiscountAuthorization : Boolean.FALSE);
        policy.setRoundingMode(
                roundingMode != null ? roundingMode : FinancialChargePolicy.RoundingModeType.HALF_UP);
    }

    private FinancialChargePolicy require(UUID id) {
        return policyRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política financeira não encontrada"));
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? policyRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : policyRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Já existe política financeira com este código na organização");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return v;
    }

    private static RoundingMode toRounding(FinancialChargePolicy.RoundingModeType mode) {
        if (mode == null) {
            return RoundingMode.HALF_UP;
        }
        return switch (mode) {
            case DOWN -> RoundingMode.DOWN;
            case UP -> RoundingMode.UP;
            case HALF_EVEN -> RoundingMode.HALF_EVEN;
            default -> RoundingMode.HALF_UP;
        };
    }

    private Response toResponse(FinancialChargePolicy p) {
        return new Response(
                p.getId(),
                p.getOrganization().getId(),
                p.getStore() != null ? p.getStore().getId() : null,
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getPriority(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getInterestType(),
                p.getInterestRate(),
                p.getInterestGraceDays(),
                p.getPenaltyType(),
                p.getPenaltyFixedAmount(),
                p.getPenaltyPercent(),
                p.getEarlyDiscountType(),
                p.getEarlyDiscountPercent(),
                p.getEarlyDiscountDays(),
                p.getMaxAuthorizedDiscountPercent(),
                p.getRequiresDiscountAuthorization(),
                p.getRoundingMode(),
                p.getStatus(),
                p.getVersion(),
                p.getCreatedAt());
    }
}
