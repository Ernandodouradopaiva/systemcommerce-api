package br.com.systemcommerce.finance.cashflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CashFlowDtos {
    private CashFlowDtos() {}

    public enum Perspective {
        REALIZED,
        PROJECTED,
        CONSOLIDATED
    }

    public record ScenarioCreateRequest(
            @NotNull UUID organizationId,
            @NotBlank String code,
            @NotBlank String name,
            String description,
            BigDecimal inflowFactor,
            BigDecimal outflowFactor) {}

    public record ScenarioResponse(
            UUID id,
            UUID organizationId,
            String code,
            String name,
            String description,
            BigDecimal inflowFactor,
            BigDecimal outflowFactor) {}

    public record CashFlowQuery(
            @NotNull UUID organizationId,
            UUID storeId,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            String timezone,
            UUID scenarioId,
            Perspective perspective) {}

    public record DayBucket(
            LocalDate date,
            BigDecimal openingBalance,
            BigDecimal inflows,
            BigDecimal outflows,
            BigDecimal dailyBalance,
            BigDecimal accumulatedBalance,
            BigDecimal projectedBalance,
            BigDecimal cashNeed,
            BigDecimal availability) {}

    public record BreakdownItem(
            String dimension,
            String key,
            String label,
            BigDecimal inflows,
            BigDecimal outflows) {}

    public record DrillDownItem(
            String sourceType,
            UUID sourceId,
            LocalDate date,
            String description,
            BigDecimal amount,
            String direction) {}

    public record CashFlowIndicators(
            BigDecimal openingBalance,
            BigDecimal totalInflows,
            BigDecimal totalOutflows,
            BigDecimal projectedBalance,
            BigDecimal cashNeed,
            BigDecimal availability) {}

    public record CashFlowResponse(
            CashFlowIndicators indicators,
            List<DayBucket> days,
            List<BreakdownItem> breakdownByHolder,
            List<BreakdownItem> breakdownByStore,
            List<BreakdownItem> breakdownByCategory,
            List<BreakdownItem> breakdownByCostCenter,
            List<DrillDownItem> drillDownSample) {}
}
