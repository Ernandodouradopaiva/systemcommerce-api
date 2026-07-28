package br.com.systemcommerce.finance.report.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class FinanceReportDtos {
    private FinanceReportDtos() {}

    public enum ReportType {
        PAYABLES,
        RECEIVABLES,
        DUE_DATES,
        DELINQUENCY,
        PAYMENTS,
        RECEIPTS,
        ACCOUNT_STATEMENT,
        CASH_FLOW,
        FORECAST,
        CATEGORIES,
        COST_CENTERS,
        SUPPLIERS,
        CUSTOMERS,
        CARDS,
        RECONCILIATION,
        TRANSFERS,
        REVERSALS,
        ADVANCES,
        RENEGOTIATIONS,
        INCOME_STATEMENT,
        STORE_POSITION
    }

    public enum ExportFormat {
        CSV,
        PDF
    }

    public record FinanceReportQuery(
            @NotNull UUID organizationId,
            UUID storeId,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            LocalDate from,
            LocalDate to,
            String status,
            String q,
            String groupBy,
            String sort,
            boolean detail,
            String timezone) {}

    public record ReportRow(
            UUID id,
            LocalDate date,
            String description,
            BigDecimal amount,
            String status,
            UUID storeId,
            UUID holderId,
            UUID categoryId,
            Map<String, Object> extra) {}
}
