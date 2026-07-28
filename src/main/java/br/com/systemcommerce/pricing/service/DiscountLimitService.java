package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditOutcome;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import br.com.systemcommerce.pricing.entity.OperatorDiscountLimit;
import br.com.systemcommerce.pricing.repository.DiscountAuthorizationRepository;
import br.com.systemcommerce.pricing.repository.DiscountPolicyRepository;
import br.com.systemcommerce.pricing.repository.OperatorDiscountLimitRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.sale.validation.SaleTotalsCalculator;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscountLimitService {

    private final OperatorDiscountLimitRepository operatorDiscountLimitRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final DiscountAuthorizationRepository discountAuthorizationRepository;
    private final UserRepository userRepository;
    private final SaleDiscountProperties saleDiscountProperties;
    private final PosAuditService posAuditService;

    public record OperatorLimitView(BigDecimal maxPercent, BigDecimal maxAmount, String roleCode) {}

    @Transactional(readOnly = true)
    public OperatorLimitView resolveOperatorLimit(UUID userId) {
        User user = userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado"));
        List<UUID> roleIds = user.getRoles().stream().map(Role::getId).toList();
        if (roleIds.isEmpty()) {
            return new OperatorLimitView(BigDecimal.ZERO, null, null);
        }
        return operatorDiscountLimitRepository.findActiveByRoleIds(roleIds).stream()
                .max(Comparator.comparing(OperatorDiscountLimit::getMaxPercent))
                .map(l -> new OperatorLimitView(
                        l.getMaxPercent(), l.getMaxAmount(), l.getRole().getCode()))
                .orElseGet(() -> new OperatorLimitView(new BigDecimal("10"), null, null));
    }

    /**
     * Valida desconto contra política (produto/categoria/global) e limite do operador.
     * Acima do limite exige POS_DISCOUNT_AUTHORIZE ou autorização APPROVED prévia.
     *
     * @return usuário autorizador gravado no item/venda, ou null se dentro do limite
     */
    @Transactional(readOnly = true)
    public User assertDiscountAllowed(
            UUID saleId,
            UUID saleItemId,
            Product product,
            BigDecimal baseAmount,
            BigDecimal discountAmount,
            UUID authorizedById) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal base = MoneyAndQuantityUtils.money(baseAmount == null ? BigDecimal.ZERO : baseAmount);
        BigDecimal discount = MoneyAndQuantityUtils.money(discountAmount);

        if (discount.compareTo(base) > 0) {
            denyDiscount(saleId, "Desconto não pode ultrapassar o valor do item ou da venda", "DISCOUNT_EXCEEDS_BASE");
            throw new BusinessRuleException("Desconto não pode ultrapassar o valor do item ou da venda");
        }
        try {
            SaleTotalsCalculator.validateHeaderDiscount(base, discount, saleDiscountProperties);
        } catch (BusinessRuleException ex) {
            denyDiscount(saleId, ex.getMessage(), "DISCOUNT_POLICY");
            throw ex;
        }

        BigDecimal percent = base.compareTo(BigDecimal.ZERO) > 0
                ? discount.multiply(new BigDecimal("100")).divide(base, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Instant now = Instant.now();
        DiscountPolicy policy = findStrictestPolicy(product, now);
        if (policy != null) {
            if (percent.compareTo(policy.getMaxPercent()) > 0) {
                String msg = "Desconto excede a política " + policy.getCode() + " (" + policy.getMaxPercent() + "%)";
                denyDiscount(saleId, msg, "DISCOUNT_POLICY_PERCENT");
                throw new BusinessRuleException(msg);
            }
            if (policy.getMaxAmount() != null && discount.compareTo(policy.getMaxAmount()) > 0) {
                String msg = "Desconto excede o valor máximo da política " + policy.getCode();
                denyDiscount(saleId, msg, "DISCOUNT_POLICY_AMOUNT");
                throw new BusinessRuleException(msg);
            }
        }

        OperatorLimitView opLimit = resolveOperatorLimit(CurrentUser.requireId());
        boolean aboveOperator = percent.compareTo(opLimit.maxPercent()) > 0
                || (opLimit.maxAmount() != null && discount.compareTo(opLimit.maxAmount()) > 0);

        if (!aboveOperator) {
            return null;
        }

        if (SecurityAuthorities.hasAuthority("POS_DISCOUNT_AUTHORIZE")) {
            return userRepository.findById(CurrentUser.requireId()).orElse(null);
        }

        var approved = discountAuthorizationRepository.findApprovedMatching(saleId, saleItemId, discount);
        if (!approved.isEmpty()) {
            DiscountAuthorization auth = approved.getFirst();
            return auth.getDecidedBy() != null
                    ? auth.getDecidedBy()
                    : userRepository.findById(authorizedById != null ? authorizedById : CurrentUser.requireId())
                            .orElse(null);
        }

        denyDiscount(
                saleId,
                "Desconto acima do limite do operador exige autorização (POS_DISCOUNT_AUTHORIZE)",
                "DISCOUNT_AUTH_REQUIRED");
        throw new BusinessRuleException(
                "Desconto acima do limite do operador exige autorização (POS_DISCOUNT_AUTHORIZE)");
    }

    private void denyDiscount(UUID saleId, String details, String errorCode) {
        // Não grava sale_id com FK se a venda ainda não existir / for referência sintética de teste.
        posAuditService.recordIndependent(
                PosAuditEventCode.DISCOUNT_DENIED,
                PosAuditOutcome.DENIED,
                PosAuditContext.builder()
                        .entity("Sale", saleId)
                        .action(br.com.systemcommerce.shared.audit.AuditLog.AuditAction.OTHER)
                        .details(details)
                        .errorCode(errorCode)
                        .build());
    }

    private DiscountPolicy findStrictestPolicy(Product product, Instant now) {
        List<DiscountPolicy> active = discountPolicyRepository.findAllActive().stream()
                .filter(p -> p.isValidAt(now))
                .toList();

        DiscountPolicy productPolicy = active.stream()
                .filter(p -> p.getAppliesTo() == DiscountPolicy.AppliesTo.PRODUCT
                        && product != null
                        && p.getProduct() != null
                        && p.getProduct().getId().equals(product.getId()))
                .min(Comparator.comparing(DiscountPolicy::getMaxPercent)
                        .thenComparing(Comparator.comparing(DiscountPolicy::getPriority).reversed()))
                .orElse(null);
        if (productPolicy != null) {
            return productPolicy;
        }

        DiscountPolicy categoryPolicy = active.stream()
                .filter(p -> p.getAppliesTo() == DiscountPolicy.AppliesTo.CATEGORY
                        && product != null
                        && product.getCategory() != null
                        && p.getCategory() != null
                        && p.getCategory().getId().equals(product.getCategory().getId()))
                .min(Comparator.comparing(DiscountPolicy::getMaxPercent)
                        .thenComparing(Comparator.comparing(DiscountPolicy::getPriority).reversed()))
                .orElse(null);
        if (categoryPolicy != null) {
            return categoryPolicy;
        }

        return active.stream()
                .filter(p -> p.getAppliesTo() == DiscountPolicy.AppliesTo.GLOBAL)
                .min(Comparator.comparing(DiscountPolicy::getMaxPercent)
                        .thenComparing(Comparator.comparing(DiscountPolicy::getPriority).reversed()))
                .orElse(null);
    }
}
