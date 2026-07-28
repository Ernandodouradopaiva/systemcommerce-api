package br.com.systemcommerce.dashboard.executive.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resposta consolidada do dashboard executivo — todos os KPIs calculados na API. */
public record ExecutiveDashboardResponse(
        Instant generatedAt,
        String timezone,
        ExecutivePerspective perspective,
        Instant periodFrom,
        Instant periodToExclusive,
        UUID organizationId,
        UUID storeId,
        UUID warehouseId,
        String channelCode,
        SalesKpi sales,
        FinancialKpi financial,
        InventoryKpi inventory,
        PurchaseKpi purchases,
        CashKpi cash,
        PeopleKpi people,
        ChannelKpi channels,
        List<DrillDownSeriesPoint> revenueSeries) {

    public record SalesKpi(
            BigDecimal revenue,
            long orderCount,
            BigDecimal averageTicket,
            BigDecimal marginAmount,
            BigDecimal marginPercent,
            BigDecimal costAmount,
            long salesOrderCount) {}

    public record FinancialKpi(
            BigDecimal receiptsConfirmed,
            long receiptCount,
            BigDecimal paymentsOut) {}

    public record InventoryKpi(
            BigDecimal stockValue,
            BigDecimal onHandUnits,
            BigDecimal availableUnits,
            long belowMinimumCount,
            long stockoutCount,
            BigDecimal turnoverRatio,
            BigDecimal coverageDays,
            BigDecimal lossAmount) {}

    public record PurchaseKpi(
            BigDecimal purchaseAmount,
            long purchaseOrderCount,
            long openPurchaseOrderCount,
            BigDecimal receiptAmount) {}

    public record CashKpi(long openSessionsCount, BigDecimal cashDifferenceTotal) {}

    public record PeopleKpi(
            long newCustomers,
            BigDecimal quoteConversionRate,
            long quotesTotal,
            long quotesConverted,
            BigDecimal sellerTargetAchievementPercent) {}

    public record ChannelKpi(
            long channelOrdersReceived,
            long channelOrdersConverted,
            BigDecimal channelRevenue) {}

    public record DrillDownSeriesPoint(String label, BigDecimal amount, long count) {}
}
