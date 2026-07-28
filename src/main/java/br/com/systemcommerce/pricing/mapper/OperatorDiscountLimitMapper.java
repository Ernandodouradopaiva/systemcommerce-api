package br.com.systemcommerce.pricing.mapper;

import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitResponse;
import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitUpsertRequest;
import br.com.systemcommerce.pricing.entity.OperatorDiscountLimit;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.user.entity.Role;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class OperatorDiscountLimitMapper {

    public OperatorDiscountLimitResponse toResponse(OperatorDiscountLimit limit) {
        Role role = limit.getRole();
        return new OperatorDiscountLimitResponse(
                limit.getId(),
                role != null ? role.getId() : null,
                role != null ? role.getCode() : null,
                role != null ? role.getName() : null,
                limit.getMaxPercent(),
                limit.getMaxAmount(),
                limit.getCreatedAt(),
                limit.getUpdatedAt(),
                limit.getVersion());
    }

    public void applyUpsert(OperatorDiscountLimit limit, OperatorDiscountLimitUpsertRequest request, Role role) {
        limit.setRole(role);
        limit.setMaxPercent(request.maxPercent().setScale(4, RoundingMode.HALF_UP));
        limit.setMaxAmount(request.maxAmount() != null ? MoneyAndQuantityUtils.money(request.maxAmount()) : null);
        limit.setActive(true);
    }
}
