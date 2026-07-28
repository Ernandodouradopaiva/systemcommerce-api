package br.com.systemcommerce.pricing.mapper;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pricing.dto.PromotionBenefitResponse;
import br.com.systemcommerce.pricing.dto.PromotionConditionResponse;
import br.com.systemcommerce.pricing.dto.PromotionCreateRequest;
import br.com.systemcommerce.pricing.dto.PromotionProductResponse;
import br.com.systemcommerce.pricing.dto.PromotionResponse;
import br.com.systemcommerce.pricing.dto.PromotionRuleResponse;
import br.com.systemcommerce.pricing.dto.PromotionUpdateRequest;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import br.com.systemcommerce.pricing.entity.PromotionCondition;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import br.com.systemcommerce.pricing.entity.PromotionRule;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    public PromotionResponse toResponse(Promotion promotion) {
        List<Store> stores = promotion.getStores() == null
                ? List.of()
                : promotion.getStores().stream().sorted(Comparator.comparing(Store::getCode)).toList();
        return new PromotionResponse(
                promotion.getId(),
                promotion.getOrganization() != null ? promotion.getOrganization().getId() : null,
                promotion.getOrganization() != null ? promotion.getOrganization().getCode() : null,
                promotion.getCode(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getChannel(),
                promotion.getStatus(),
                promotion.getPriority(),
                promotion.getValidFrom(),
                promotion.getValidTo(),
                stores.stream().map(Store::getId).toList(),
                stores.stream().map(Store::getCode).toList(),
                promotion.getPromotionType(),
                promotion.isStackable(),
                promotion.getMinOrderAmount(),
                promotion.getBrand() != null ? promotion.getBrand().getId() : null,
                promotion.getCategory() != null ? promotion.getCategory().getId() : null,
                promotion.getCreatedAt(),
                promotion.getUpdatedAt(),
                promotion.getVersion());
    }

    public PromotionRuleResponse toRuleResponse(PromotionRule rule) {
        return new PromotionRuleResponse(
                rule.getId(),
                rule.getPromotion() != null ? rule.getPromotion().getId() : null,
                rule.getRuleType(),
                rule.getConfigJson(),
                rule.getSortOrder());
    }

    public PromotionConditionResponse toConditionResponse(PromotionCondition condition) {
        return new PromotionConditionResponse(
                condition.getId(),
                condition.getPromotion() != null ? condition.getPromotion().getId() : null,
                condition.getConditionType(),
                condition.getReferenceId(),
                condition.getMinQuantity(),
                condition.getMinAmount(),
                condition.getConfigJson());
    }

    public PromotionBenefitResponse toBenefitResponse(PromotionBenefit benefit) {
        return new PromotionBenefitResponse(
                benefit.getId(),
                benefit.getPromotion() != null ? benefit.getPromotion().getId() : null,
                benefit.getBenefitType(),
                benefit.getPercentValue(),
                benefit.getFixedValue(),
                benefit.getPromoUnitPrice(),
                benefit.getBuyQuantity(),
                benefit.getPayQuantity(),
                benefit.getMaxBenefitAmount());
    }

    public PromotionProductResponse toProductResponse(PromotionProduct item) {
        Promotion promotion = item.getPromotion();
        var product = item.getProduct();
        return new PromotionProductResponse(
                item.getId(),
                promotion != null ? promotion.getId() : null,
                promotion != null ? promotion.getCode() : null,
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                item.getPromotionalPrice(),
                item.getMinQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getVersion());
    }

    public void applyCreate(Promotion promotion, PromotionCreateRequest request) {
        promotion.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        promotion.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        promotion.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        promotion.setChannel(request.channel() != null ? request.channel() : br.com.systemcommerce.pricing.entity.PriceChannel.POS);
        promotion.setPriority(request.priority() != null ? request.priority() : 100);
        promotion.setValidFrom(request.validFrom());
        promotion.setValidTo(request.validTo());
        promotion.setStatus(Promotion.Status.ACTIVE);
        promotion.setActive(true);
        promotion.setPromotionType(request.promotionType());
        promotion.setStackable(Boolean.TRUE.equals(request.stackable()));
        promotion.setMinOrderAmount(request.minOrderAmount());
    }

    public void applyUpdate(Promotion promotion, PromotionUpdateRequest request) {
        promotion.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        promotion.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        promotion.setChannel(request.channel());
        promotion.setPriority(request.priority());
        promotion.setStatus(request.status());
        promotion.setValidFrom(request.validFrom());
        promotion.setValidTo(request.validTo());
        promotion.setPromotionType(request.promotionType());
        promotion.setStackable(Boolean.TRUE.equals(request.stackable()));
        promotion.setMinOrderAmount(request.minOrderAmount());
    }
}
