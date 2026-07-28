package br.com.systemcommerce.pricing.mapper;

import br.com.systemcommerce.pricing.dto.DiscountPolicyCreateRequest;
import br.com.systemcommerce.pricing.dto.DiscountPolicyResponse;
import br.com.systemcommerce.pricing.dto.DiscountPolicyUpdateRequest;
import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class DiscountPolicyMapper {

    public DiscountPolicyResponse toResponse(DiscountPolicy policy) {
        Product product = policy.getProduct();
        Category category = policy.getCategory();
        return new DiscountPolicyResponse(
                policy.getId(),
                policy.getCode(),
                policy.getName(),
                policy.getDescription(),
                policy.getAppliesTo(),
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                policy.getMaxPercent(),
                policy.getMaxAmount(),
                policy.getPriority(),
                policy.getStatus(),
                policy.getValidFrom(),
                policy.getValidTo(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                policy.getVersion());
    }

    public void applyCreate(
            DiscountPolicy policy,
            DiscountPolicyCreateRequest request,
            Product product,
            Category category) {
        policy.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        policy.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        policy.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        policy.setAppliesTo(request.appliesTo());
        policy.setProduct(product);
        policy.setCategory(category);
        policy.setMaxPercent(request.maxPercent().setScale(4, java.math.RoundingMode.HALF_UP));
        policy.setMaxAmount(request.maxAmount() != null ? MoneyAndQuantityUtils.money(request.maxAmount()) : null);
        policy.setPriority(request.priority() != null ? request.priority() : 0);
        policy.setValidFrom(request.validFrom());
        policy.setValidTo(request.validTo());
        policy.setStatus(DiscountPolicy.Status.ACTIVE);
        policy.setActive(true);
    }

    public void applyUpdate(
            DiscountPolicy policy,
            DiscountPolicyUpdateRequest request,
            Product product,
            Category category) {
        policy.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        policy.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        policy.setAppliesTo(request.appliesTo());
        policy.setProduct(product);
        policy.setCategory(category);
        policy.setMaxPercent(request.maxPercent().setScale(4, java.math.RoundingMode.HALF_UP));
        policy.setMaxAmount(request.maxAmount() != null ? MoneyAndQuantityUtils.money(request.maxAmount()) : null);
        policy.setPriority(request.priority() != null ? request.priority() : 0);
        policy.setStatus(request.status());
        policy.setValidFrom(request.validFrom());
        policy.setValidTo(request.validTo());
    }
}
