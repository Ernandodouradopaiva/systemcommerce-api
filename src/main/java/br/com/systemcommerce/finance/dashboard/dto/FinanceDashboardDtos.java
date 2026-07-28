package br.com.systemcommerce.finance.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceDashboardDtos {
    private FinanceDashboardDtos() {}

    public enum DashboardMetric {
        AVAILABLE_BALANCE,
        BALANCES_BY_ACCOUNT,
        PAYABLES_DUE_TODAY,
        RECEIVABLES_DUE_TODAY,
        OVERDUE_PAYABLES,
        OVERDUE_RECEIVABLES,
        PAYMENTS_IN_PERIOD,
        RECEIPTS_IN_PERIOD,
        EXPENSES_BY_CATEGORY,
        REVENUES_BY_CATEGORY,
        CARDS_RECEIVABLE,
        PENDING_RECONCILIATIONS,
        OPEN_CASH_SESSIONS,
        CASH_DIFFERENCES,
        TOP_SUPPLIERS_BY_PAYMENT,
        TOP_CUSTOMERS_BY_BALANCE
    }

    public record FinanceDashboardQuery(
            @NotNull UUID organizationId,
            UUID storeId,
            UUID storeGroupId,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            LocalDate from,
            LocalDate to,
            String timezone) {}

    public record AccountBalance(UUID holderId, String code, String name, BigDecimal balance) {}

    public record CategoryAmount(UUID categoryId, String categoryName, BigDecimal amount) {}

    public record RankedEntity(UUID id, String label, BigDecimal amount) {}

    public record FinanceDashboardResponse(
            BigDecimal availableBalance,
            List<AccountBalance> balancesByAccount,
            BigDecimal payablesDueToday,
            BigDecimal receivablesDueToday,
            BigDecimal overduePayables,
            BigDecimal overdueReceivables,
            BigDecimal delinquencyRate,
            BigDecimal projectedCashFlow7,
            BigDecimal projectedCashFlow15,
            BigDecimal projectedCashFlow30,
            BigDecimal projectedCashFlow60,
            BigDecimal projectedCashFlow90,
            BigDecimal paymentsInPeriod,
            BigDecimal receiptsInPeriod,
            List<CategoryAmount> expensesByCategory,
            List<CategoryAmount> revenuesByCategory,
            BigDecimal managerialResult,
            BigDecimal cardsReceivable,
            long pendingReconciliations,
            long openCashSessions,
            BigDecimal cashDifferences,
            List<RankedEntity> topSuppliersByPayment,
            List<RankedEntity> topCustomersByBalance,
            Instant refreshedAt) {}

    public record DrillDownItem(UUID id, String label, BigDecimal amount, LocalDate date) {}
}
