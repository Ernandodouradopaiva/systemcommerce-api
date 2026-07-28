package br.com.systemcommerce.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionEngineServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PromotionApplicationRepository promotionApplicationRepository;

    private PromotionEngineService service;

    private final UUID storeId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PromotionEngineService(
                promotionRepository, couponRepository, promotionApplicationRepository, new ObjectMapper());
    }

    @Test
    void shouldApplyPercentDiscountOverSubtotal() {
        Promotion promotion = promotion("PROMO10", 100, false);
        addBenefit(promotion, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "10", null, null, null, null, null));
        mockCandidates(promotion);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("2"), new BigDecimal("50"))));

        assertThat(result.subtotal()).isEqualByComparingTo("100.00");
        assertThat(result.totalDiscount()).isEqualByComparingTo("10.00");
        assertThat(result.total()).isEqualByComparingTo("90.00");
        assertThat(result.applications()).hasSize(1);
    }

    @Test
    void shouldCapFixedDiscountByMaxBenefitAmount() {
        Promotion promotion = promotion("PROMO-FIXED", 100, false);
        addBenefit(
                promotion,
                benefit(PromotionBenefit.BenefitType.FIXED_DISCOUNT, null, "50", null, null, null, "20"));
        mockCandidates(promotion);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("1"), new BigDecimal("100"))));

        assertThat(result.totalDiscount()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldApplyBuyThreePayTwoDiscount() {
        Promotion promotion = promotion("B3P2", 100, false);
        addBenefit(
                promotion,
                benefit(PromotionBenefit.BenefitType.BUY_X_PAY_Y, null, null, null, "3", "2", null));
        mockCandidates(promotion);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("6"), new BigDecimal("10"))));

        // 6 unidades = 2 conjuntos de 3; cada conjunto dá 1 unidade grátis (10) => desconto 20
        assertThat(result.totalDiscount()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldApplyOnlyHigherPriorityWhenPromotionsAreNotStackable() {
        Promotion low = promotion("LOW", 10, false);
        addBenefit(low, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "5", null, null, null, null, null));
        Promotion high = promotion("HIGH", 50, false);
        addBenefit(high, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "20", null, null, null, null, null));
        mockCandidates(low, high);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("1"), new BigDecimal("100"))));

        assertThat(result.applications()).hasSize(1);
        assertThat(result.applications().getFirst().promotionCode()).isEqualTo("HIGH");
        assertThat(result.totalDiscount()).isEqualByComparingTo("20.00");
    }

    @Test
    void shouldStackDiscountsWhenPromotionsAreStackable() {
        Promotion first = promotion("STACK1", 50, true);
        addBenefit(first, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "10", null, null, null, null, null));
        Promotion second = promotion("STACK2", 30, true);
        addBenefit(second, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "5", null, null, null, null, null));
        mockCandidates(first, second);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("1"), new BigDecimal("100"))));

        assertThat(result.applications()).hasSize(2);
        assertThat(result.totalDiscount()).isEqualByComparingTo("15.00");
    }

    @Test
    void shouldNotApplyPromotionRequiringCouponWithoutValidCode() {
        Promotion promotion = promotion("CUPOM10", 100, false);
        addBenefit(promotion, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "10", null, null, null, null, null));
        addCouponCondition(promotion);
        mockCandidates(promotion);

        PromotionEngineResultResponse result = service.apply(cart(cartItem(new BigDecimal("1"), new BigDecimal("100"))));

        assertThat(result.applications()).isEmpty();
        assertThat(result.totalDiscount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldApplyPromotionRequiringCouponWhenValidCodeProvided() {
        Promotion promotion = promotion("CUPOM10", 100, false);
        addBenefit(promotion, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "10", null, null, null, null, null));
        addCouponCondition(promotion);
        mockCandidates(promotion);
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE10");
        coupon.setStatus(Coupon.Status.ACTIVE);
        coupon.setActive(true);
        when(couponRepository.findFirstByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        PromotionCartContextRequest context = new PromotionCartContextRequest(
                storeId, PriceChannel.POS, null, null, "SAVE10", List.of(cartItem(new BigDecimal("1"), new BigDecimal("100"))));
        PromotionEngineResultResponse result = service.apply(context);

        assertThat(result.applications()).hasSize(1);
        assertThat(result.couponApplied()).isTrue();
        assertThat(result.totalDiscount()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldPersistApplicationsAndRegisterCouponUseOnApplyAndRecord() {
        Promotion promotion = promotion("CUPOM10", 100, false);
        addBenefit(promotion, benefit(PromotionBenefit.BenefitType.PERCENT_DISCOUNT, "10", null, null, null, null, null));
        addCouponCondition(promotion);
        mockCandidates(promotion);
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("SAVE10");
        coupon.setStatus(Coupon.Status.ACTIVE);
        coupon.setActive(true);
        coupon.setUsedCount(0);
        when(couponRepository.findFirstByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));
        when(promotionApplicationRepository.save(any(PromotionApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionCartContextRequest context = new PromotionCartContextRequest(
                storeId, PriceChannel.POS, null, null, "SAVE10", List.of(cartItem(new BigDecimal("1"), new BigDecimal("100"))));
        service.applyAndRecord(context, UUID.randomUUID(), null, null, null);

        verify(promotionApplicationRepository, times(1)).save(any(PromotionApplication.class));
        verify(couponRepository, times(1)).save(any(Coupon.class));
        assertThat(coupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    void shouldNotSaveAnythingWhenNoPromotionApplies() {
        mockCandidates();

        PromotionCartContextRequest context = cart(cartItem(new BigDecimal("1"), new BigDecimal("100")));
        service.applyAndRecord(context, UUID.randomUUID(), null, null, null);

        verify(promotionApplicationRepository, never()).save(any());
    }

    private void mockCandidates(Promotion... promotions) {
        when(promotionRepository.findEngineCandidates(any(), any())).thenReturn(List.of(promotions));
    }

    private PromotionCartContextRequest cart(PromotionCartItemRequest... items) {
        return new PromotionCartContextRequest(storeId, PriceChannel.POS, null, null, null, List.of(items));
    }

    private PromotionCartItemRequest cartItem(BigDecimal quantity, BigDecimal unitPrice) {
        return new PromotionCartItemRequest(productId, null, null, quantity, unitPrice);
    }

    private Promotion promotion(String code, int priority, boolean stackable) {
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setCode(code);
        promotion.setName(code);
        promotion.setPriority(priority);
        promotion.setStackable(stackable);
        promotion.setActive(true);
        promotion.setStatus(Promotion.Status.ACTIVE);
        promotion.setChannel(PriceChannel.POS);
        promotion.setPromotionType(Promotion.PromotionType.PERCENT_DISCOUNT);
        promotion.setStores(new HashSet<>());
        promotion.setBenefits(new HashSet<>());
        promotion.setConditions(new HashSet<>());
        return promotion;
    }

    private void addBenefit(
            Promotion promotion,
            PromotionBenefit benefit) {
        benefit.setPromotion(promotion);
        promotion.getBenefits().add(benefit);
    }

    private PromotionBenefit benefit(
            PromotionBenefit.BenefitType type,
            String percent,
            String fixed,
            String promoUnitPrice,
            String buyQty,
            String payQty,
            String maxBenefit) {
        PromotionBenefit benefit = new PromotionBenefit();
        benefit.setId(UUID.randomUUID());
        benefit.setBenefitType(type);
        benefit.setActive(true);
        if (percent != null) benefit.setPercentValue(new BigDecimal(percent));
        if (fixed != null) benefit.setFixedValue(new BigDecimal(fixed));
        if (promoUnitPrice != null) benefit.setPromoUnitPrice(new BigDecimal(promoUnitPrice));
        if (buyQty != null) benefit.setBuyQuantity(new BigDecimal(buyQty));
        if (payQty != null) benefit.setPayQuantity(new BigDecimal(payQty));
        if (maxBenefit != null) benefit.setMaxBenefitAmount(new BigDecimal(maxBenefit));
        return benefit;
    }

    private void addCouponCondition(Promotion promotion) {
        PromotionCondition condition = new PromotionCondition();
        condition.setId(UUID.randomUUID());
        condition.setPromotion(promotion);
        condition.setConditionType(PromotionCondition.ConditionType.COUPON);
        condition.setActive(true);
        promotion.getConditions().add(condition);
    }
}
