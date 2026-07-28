package br.com.systemcommerce.finance.policy.dto;

import br.com.systemcommerce.finance.policy.entity.FinancialChargePolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FinancialChargePolicyDtos {
    private FinancialChargePolicyDtos() {}

    public record CreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            Integer priority,
            @NotNull LocalDate validFrom,
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
            FinancialChargePolicy.RoundingModeType roundingMode) {}

    public record UpdateRequest(
            @NotBlank String name,
            String description,
            Integer priority,
            @NotNull LocalDate validFrom,
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
            FinancialChargePolicy.RoundingModeType roundingMode,
            FinancialChargePolicy.Status status) {}

    public record SimulateRequest(
            @NotNull LocalDate baseDate,
            @NotNull @DecimalMin("0.00") BigDecimal principal,
            @NotNull LocalDate dueDate,
            @NotNull LocalDate payDate,
            @NotNull UUID policyId,
            BigDecimal authorizedDiscount) {}

    public record SimulateResponse(
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal penalty,
            BigDecimal discount,
            BigDecimal total,
            UUID policyId,
            String policyCode) {}

    public record Response(
            UUID id,
            UUID organizationId,
            UUID storeId,
            String code,
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
            FinancialChargePolicy.RoundingModeType roundingMode,
            FinancialChargePolicy.Status status,
            Long version,
            Instant createdAt) {}
}
