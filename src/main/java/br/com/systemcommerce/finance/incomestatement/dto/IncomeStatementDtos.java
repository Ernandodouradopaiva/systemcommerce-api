package br.com.systemcommerce.finance.incomestatement.dto;

import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementExecution;
import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementLine;
import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class IncomeStatementDtos {
    private IncomeStatementDtos() {}

    public record LayoutResponse(
            UUID id,
            UUID organizationId,
            String code,
            String name,
            String description,
            List<LineResponse> lines) {}

    public record LineResponse(
            UUID id,
            String code,
            String name,
            String lineType,
            Integer sortOrder,
            String formula,
            String formulaDoc,
            Integer signMultiplier) {}

    public record MappingCreateRequest(
            @NotNull UUID lineId,
            UUID financialCategoryId,
            UUID financialAccountId,
            @NotNull IncomeStatementMapping.SourceType sourceType) {}

    public record MappingResponse(
            UUID id,
            UUID lineId,
            UUID financialCategoryId,
            UUID financialAccountId,
            String sourceType) {}

    public record ExecuteRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull UUID layoutId,
            @NotNull IncomeStatementExecution.Basis basis,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            LocalDate compareFrom,
            LocalDate compareTo,
            String timezone,
            String notes) {}

    public record ExecutionLineResponse(
            UUID lineId,
            String lineCode,
            String lineName,
            String lineType,
            BigDecimal amount,
            BigDecimal compareAmount,
            BigDecimal varianceAmount,
            String formulaApplied,
            Integer sortOrder) {}

    public record ExecutionResponse(
            UUID id,
            UUID organizationId,
            UUID storeId,
            UUID layoutId,
            String basis,
            LocalDate periodFrom,
            LocalDate periodTo,
            LocalDate compareFrom,
            LocalDate compareTo,
            String timezone,
            Instant executedAt,
            UUID executedBy,
            String notes,
            List<ExecutionLineResponse> lines) {}

    public record DrillDownItem(
            String sourceType,
            UUID sourceId,
            LocalDate date,
            String description,
            BigDecimal amount) {}

    public record DrillDownQuery(
            @NotNull UUID executionId,
            @NotBlank String lineCode,
            Integer limit) {}
}
