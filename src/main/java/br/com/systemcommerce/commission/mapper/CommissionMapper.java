package br.com.systemcommerce.commission.mapper;

import br.com.systemcommerce.commission.dto.CommissionAdjustmentResponse;
import br.com.systemcommerce.commission.dto.CommissionCalculationResponse;
import br.com.systemcommerce.commission.dto.CommissionPolicyCreateRequest;
import br.com.systemcommerce.commission.dto.CommissionPolicyResponse;
import br.com.systemcommerce.commission.dto.SalesTargetCreateRequest;
import br.com.systemcommerce.commission.dto.SalesTargetResponse;
import br.com.systemcommerce.commission.entity.CommissionAdjustment;
import br.com.systemcommerce.commission.entity.CommissionCalculation;
import br.com.systemcommerce.commission.entity.CommissionPolicy;
import br.com.systemcommerce.commission.entity.SalesTarget;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CommissionMapper {

    public SalesTargetResponse toResponse(SalesTarget target) {
        return new SalesTargetResponse(
                target.getId(),
                target.getOrganization() != null ? target.getOrganization().getId() : null,
                target.getSellerProfile() != null ? target.getSellerProfile().getId() : null,
                target.getSellerProfile() != null ? target.getSellerProfile().getSellerCode() : null,
                target.getStore() != null ? target.getStore().getId() : null,
                target.getStore() != null ? target.getStore().getCode() : null,
                target.getPeriodStart(),
                target.getPeriodEnd(),
                target.getCategory() != null ? target.getCategory().getId() : null,
                target.getProduct() != null ? target.getProduct().getId() : null,
                target.getTargetAmount(),
                target.getTargetQuantity(),
                target.getStatus(),
                target.getCreatedAt(),
                target.getUpdatedAt(),
                target.getVersion());
    }

    public CommissionPolicyResponse toPolicyResponse(CommissionPolicy policy) {
        return new CommissionPolicyResponse(
                policy.getId(),
                policy.getOrganization() != null ? policy.getOrganization().getId() : null,
                policy.getCode(),
                policy.getName(),
                policy.getPolicyVersion(),
                policy.getStore() != null ? policy.getStore().getId() : null,
                policy.getSellerProfile() != null ? policy.getSellerProfile().getId() : null,
                policy.getProduct() != null ? policy.getProduct().getId() : null,
                policy.getCategory() != null ? policy.getCategory().getId() : null,
                policy.getChannel(),
                policy.getPercent(),
                policy.getFixedAmount(),
                policy.isRequiresPaid(),
                policy.isAppliesOnConfirmed(),
                policy.getValidFrom(),
                policy.getValidTo(),
                policy.getStatus(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                policy.getVersion());
    }

    public CommissionCalculationResponse toCalculationResponse(CommissionCalculation calc) {
        return new CommissionCalculationResponse(
                calc.getId(),
                calc.getSale() != null ? calc.getSale().getId() : null,
                calc.getSale() != null ? calc.getSale().getSaleNumber() : null,
                calc.getSaleItem() != null ? calc.getSaleItem().getId() : null,
                calc.getSellerProfile() != null ? calc.getSellerProfile().getId() : null,
                calc.getSellerProfile() != null ? calc.getSellerProfile().getSellerCode() : null,
                calc.getStore() != null ? calc.getStore().getId() : null,
                calc.getPolicy() != null ? calc.getPolicy().getId() : null,
                calc.getPolicy() != null ? calc.getPolicy().getCode() : null,
                calc.getPolicyVersion(),
                calc.getBaseAmount(),
                calc.getCommissionAmount(),
                calc.getStatus(),
                calc.getCalculatedAt(),
                calc.getCreatedAt(),
                calc.getVersion());
    }

    public CommissionAdjustmentResponse toAdjustmentResponse(CommissionAdjustment adjustment) {
        return new CommissionAdjustmentResponse(
                adjustment.getId(),
                adjustment.getCalculation() != null ? adjustment.getCalculation().getId() : null,
                adjustment.getAmount(),
                adjustment.getReason(),
                adjustment.getCreatedBy());
    }

    public void applyTargetCreate(SalesTarget target, SalesTargetCreateRequest request) {
        target.setPeriodStart(request.periodStart());
        target.setPeriodEnd(request.periodEnd());
        target.setTargetAmount(MoneyAndQuantityUtils.money(request.targetAmount()));
        target.setTargetQuantity(
                request.targetQuantity() != null
                        ? MoneyAndQuantityUtils.quantity(request.targetQuantity())
                        : BigDecimal.ZERO);
        target.setStatus(SalesTarget.TargetStatus.ACTIVE);
    }

    public void applyPolicyCreate(CommissionPolicy policy, CommissionPolicyCreateRequest request) {
        policy.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        policy.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        policy.setChannel(request.channel() != null ? request.channel() : CommissionPolicy.PolicyChannel.ANY);
        policy.setPercent(MoneyAndQuantityUtils.money(request.percent()));
        policy.setFixedAmount(
                request.fixedAmount() != null
                        ? MoneyAndQuantityUtils.money(request.fixedAmount())
                        : BigDecimal.ZERO);
        policy.setRequiresPaid(Boolean.TRUE.equals(request.requiresPaid()));
        policy.setAppliesOnConfirmed(request.appliesOnConfirmed() == null || request.appliesOnConfirmed());
        policy.setValidFrom(request.validFrom());
        policy.setValidTo(request.validTo());
        policy.setStatus(CommissionPolicy.PolicyStatus.ACTIVE);
        policy.setPolicyVersion(1);
    }
}
