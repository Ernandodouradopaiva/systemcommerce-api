package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.PromotionApplicationResult;
import br.com.systemcommerce.pricing.dto.PromotionCartContextRequest;
import br.com.systemcommerce.pricing.dto.PromotionCartItemRequest;
import br.com.systemcommerce.pricing.dto.PromotionEngineResultResponse;
import br.com.systemcommerce.pricing.entity.Coupon;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.entity.PromotionApplication;
import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import br.com.systemcommerce.pricing.entity.PromotionCondition;
import br.com.systemcommerce.pricing.repository.CouponRepository;
import br.com.systemcommerce.pricing.repository.PromotionApplicationRepository;
import br.com.systemcommerce.pricing.repository.PromotionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de promoções do carrinho (Prompt 69). Avalia promoções ativas do tipo "engine" (com
 * {@link br.com.systemcommerce.pricing.entity.PromotionRule}/{@link PromotionCondition}/{@link PromotionBenefit})
 * contra um contexto de carrinho e calcula os descontos aplicáveis, respeitando prioridade, empilhamento
 * (stackable) e teto de benefício.
 *
 * <p>Esta é a fonte oficial de cálculo de desconto promocional agregado do carrinho; o front-end apenas exibe
 * o resultado retornado por esta API.
 */
@Service
@RequiredArgsConstructor
public class PromotionEngineService {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final PromotionApplicationRepository promotionApplicationRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PromotionEngineResultResponse apply(PromotionCartContextRequest context) {
        return evaluate(context, Instant.now()).result();
    }

    /**
     * Igual a {@link #apply(PromotionCartContextRequest)}, mas persiste um snapshot
     * {@link PromotionApplication} para cada promoção efetivamente aplicada, vinculado à venda/pedido/orçamento
     * informado, e registra o uso do cupom quando aplicável.
     */
    @Transactional
    public PromotionEngineResultResponse applyAndRecord(
            PromotionCartContextRequest context, UUID saleId, UUID salesOrderId, UUID quoteId, UUID appliedBy) {
        Evaluation evaluation = evaluate(context, Instant.now());
        for (AppliedPromotion applied : evaluation.applied()) {
            PromotionApplication application = new PromotionApplication();
            application.setPromotion(applied.promotion());
            application.setSaleId(saleId);
            application.setSalesOrderId(salesOrderId);
            application.setQuoteId(quoteId);
            application.setCoupon(evaluation.coupon());
            application.setBenefitAmount(applied.amount());
            application.setSnapshotJson(toJson(applied));
            application.setAppliedBy(appliedBy);
            promotionApplicationRepository.save(application);
        }
        if (evaluation.coupon() != null && !evaluation.applied().isEmpty()) {
            evaluation.coupon().registerUse();
            couponRepository.save(evaluation.coupon());
        }
        return evaluation.result();
    }

    private Evaluation evaluate(PromotionCartContextRequest context, Instant at) {
        PriceChannel channel = context.channel() != null ? context.channel() : PriceChannel.POS;
        BigDecimal subtotal = context.items().stream()
                .map(PromotionCartItemRequest::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Coupon coupon = null;
        String couponRejectionReason = null;
        if (context.couponCode() != null && !context.couponCode().isBlank()) {
            coupon = couponRepository.findFirstByCodeIgnoreCase(context.couponCode().trim()).orElse(null);
            if (coupon == null) {
                couponRejectionReason = "Cupom inválido";
            } else if (!coupon.isUsable(at)) {
                couponRejectionReason = "Cupom expirado, inativo ou esgotado";
                coupon = null;
            }
        }

        List<Promotion> candidates = promotionRepository.findEngineCandidates(channel, at).stream()
                .filter(p -> matchesStore(p, context.storeId()))
                .sorted(Comparator.comparingInt((Promotion p) -> p.getPriority() != null ? p.getPriority() : 0)
                        .reversed())
                .toList();

        List<AppliedPromotion> applied = new ArrayList<>();
        boolean nonStackableApplied = false;
        boolean couponUsedByAny = false;
        for (Promotion promotion : candidates) {
            if (!promotion.isStackable() && nonStackableApplied) {
                continue;
            }
            EvalContext evalContext = new EvalContext(context, subtotal, coupon, at);
            if (!matchesConditions(promotion, evalContext)) {
                continue;
            }
            BigDecimal amount = computeBenefitAmount(promotion, evalContext);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (requiresCoupon(promotion)) {
                couponUsedByAny = true;
            }
            applied.add(new AppliedPromotion(promotion, amount));
            if (!promotion.isStackable()) {
                nonStackableApplied = true;
            }
        }

        BigDecimal totalDiscount = applied.stream()
                .map(AppliedPromotion::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDiscount.compareTo(subtotal) > 0) {
            totalDiscount = subtotal;
        }
        BigDecimal total = subtotal.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP);

        List<PromotionApplicationResult> applicationResults = applied.stream()
                .map(a -> new PromotionApplicationResult(
                        a.promotion().getId(),
                        a.promotion().getCode(),
                        a.promotion().getName(),
                        a.amount().setScale(2, RoundingMode.HALF_UP),
                        describeBenefit(a.promotion())))
                .toList();

        boolean couponApplied = coupon != null && couponUsedByAny;
        String finalRejection = couponRejectionReason;
        if (coupon != null && !couponUsedByAny) {
            finalRejection = "Cupom válido, mas nenhuma promoção elegível o utiliza no carrinho informado";
        }

        PromotionEngineResultResponse result = new PromotionEngineResultResponse(
                subtotal, totalDiscount.setScale(2, RoundingMode.HALF_UP), total, applicationResults, couponApplied, finalRejection);
        return new Evaluation(result, applied, couponApplied ? coupon : null);
    }

    private boolean requiresCoupon(Promotion promotion) {
        return promotion.getConditions() != null
                && promotion.getConditions().stream()
                        .anyMatch(c -> c.getConditionType() == PromotionCondition.ConditionType.COUPON);
    }

    private boolean matchesStore(Promotion promotion, UUID storeId) {
        return promotion.getStores() == null
                || promotion.getStores().isEmpty()
                || promotion.getStores().stream().anyMatch(s -> s.getId().equals(storeId));
    }

    private boolean matchesConditions(Promotion promotion, EvalContext ctx) {
        if (promotion.getMinOrderAmount() != null && ctx.subtotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            return false;
        }
        if (promotion.getBrand() != null
                && ctx.context.items().stream().noneMatch(i -> promotion.getBrand().getId().equals(i.brandId()))) {
            return false;
        }
        if (promotion.getCategory() != null
                && ctx.context.items().stream()
                        .noneMatch(i -> promotion.getCategory().getId().equals(i.categoryId()))) {
            return false;
        }
        if (promotion.getConditions() == null) {
            return true;
        }
        for (PromotionCondition condition : promotion.getConditions()) {
            if (Boolean.FALSE.equals(condition.getActive())) {
                continue;
            }
            if (!matchesCondition(condition, ctx)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesCondition(PromotionCondition condition, EvalContext ctx) {
        switch (condition.getConditionType()) {
            case MIN_AMOUNT:
                return condition.getMinAmount() == null || ctx.subtotal.compareTo(condition.getMinAmount()) >= 0;
            case MIN_QUANTITY:
                BigDecimal totalQty = ctx.context.items().stream()
                        .map(PromotionCartItemRequest::quantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return condition.getMinQuantity() == null || totalQty.compareTo(condition.getMinQuantity()) >= 0;
            case CATEGORY:
                return ctx.context.items().stream()
                        .anyMatch(i -> condition.getReferenceId() != null && condition.getReferenceId().equals(i.categoryId()));
            case BRAND:
                return ctx.context.items().stream()
                        .anyMatch(i -> condition.getReferenceId() != null && condition.getReferenceId().equals(i.brandId()));
            case CUSTOMER_GROUP:
                return condition.getConfigJson() == null
                        || condition.getConfigJson().equalsIgnoreCase(ctx.context.customerGroupCode());
            case COUPON:
                return ctx.coupon != null;
            case PRODUCT:
                return ctx.context.items().stream()
                        .anyMatch(i -> condition.getReferenceId() != null && condition.getReferenceId().equals(i.productId()));
            default:
                return true;
        }
    }

    private BigDecimal computeBenefitAmount(Promotion promotion, EvalContext ctx) {
        if (promotion.getBenefits() == null || promotion.getBenefits().isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<PromotionCartItemRequest> scopedItems = scopedItems(promotion, ctx);
        BigDecimal scopedBase = scopedItems.stream()
                .map(PromotionCartItemRequest::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = BigDecimal.ZERO;
        for (PromotionBenefit benefit : promotion.getBenefits()) {
            if (Boolean.FALSE.equals(benefit.getActive())) {
                continue;
            }
            BigDecimal amount = switch (benefit.getBenefitType()) {
                case PERCENT_DISCOUNT -> benefit.getPercentValue() == null
                        ? BigDecimal.ZERO
                        : scopedBase.multiply(benefit.getPercentValue())
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                case FIXED_DISCOUNT -> benefit.getFixedValue() == null
                        ? BigDecimal.ZERO
                        : benefit.getFixedValue().min(scopedBase);
                case PROMO_PRICE -> computePromoPriceDiscount(benefit, scopedItems);
                case BUY_X_PAY_Y -> computeBuyXPayYDiscount(benefit, scopedItems);
            };
            if (benefit.getMaxBenefitAmount() != null) {
                amount = amount.min(benefit.getMaxBenefitAmount());
            }
            total = total.add(amount);
        }
        return total.min(scopedBase).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computePromoPriceDiscount(PromotionBenefit benefit, List<PromotionCartItemRequest> items) {
        if (benefit.getPromoUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = BigDecimal.ZERO;
        for (PromotionCartItemRequest item : items) {
            BigDecimal diff = item.unitPrice().subtract(benefit.getPromoUnitPrice());
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.add(diff.multiply(item.quantity()));
            }
        }
        return discount;
    }

    private BigDecimal computeBuyXPayYDiscount(PromotionBenefit benefit, List<PromotionCartItemRequest> items) {
        if (benefit.getBuyQuantity() == null
                || benefit.getPayQuantity() == null
                || benefit.getBuyQuantity().compareTo(BigDecimal.ZERO) <= 0
                || benefit.getPayQuantity().compareTo(benefit.getBuyQuantity()) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalQty = items.stream().map(PromotionCartItemRequest::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalAmount = items.stream().map(PromotionCartItemRequest::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgUnitPrice = totalAmount.divide(totalQty, 6, RoundingMode.HALF_UP);
        BigDecimal sets = totalQty.divideToIntegralValue(benefit.getBuyQuantity());
        if (sets.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal freeQtyPerSet = benefit.getBuyQuantity().subtract(benefit.getPayQuantity());
        return sets.multiply(freeQtyPerSet).multiply(avgUnitPrice);
    }

    private List<PromotionCartItemRequest> scopedItems(Promotion promotion, EvalContext ctx) {
        if (promotion.getBrand() == null && promotion.getCategory() == null) {
            return ctx.context.items();
        }
        return ctx.context.items().stream()
                .filter(i -> promotion.getBrand() == null || promotion.getBrand().getId().equals(i.brandId()))
                .filter(i -> promotion.getCategory() == null || promotion.getCategory().getId().equals(i.categoryId()))
                .toList();
    }

    private String describeBenefit(Promotion promotion) {
        if (promotion.getPromotionType() == null) {
            return promotion.getName();
        }
        return promotion.getName() + " (" + promotion.getPromotionType() + ")";
    }

    private String toJson(AppliedPromotion applied) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("promotionId", applied.promotion().getId());
        snapshot.put("promotionCode", applied.promotion().getCode());
        snapshot.put("promotionType", applied.promotion().getPromotionType());
        snapshot.put("benefitAmount", applied.amount());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record EvalContext(PromotionCartContextRequest context, BigDecimal subtotal, Coupon coupon, Instant at) {}

    private record AppliedPromotion(Promotion promotion, BigDecimal amount) {}

    private record Evaluation(
            PromotionEngineResultResponse result, List<AppliedPromotion> applied, Coupon coupon) {}
}
