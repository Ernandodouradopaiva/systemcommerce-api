package br.com.systemcommerce.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        Instant generatedAt,
        Instant dayFrom,
        Instant dayToExclusive,
        Instant monthFrom,
        Instant monthToExclusive,
        MoneyCountMetric salesToday,
        MoneyCountMetric salesMonth,
        BigDecimal averageTicketMonth,
        List<NamedAmountMetric> topProducts,
        List<NamedAmountMetric> topCustomers,
        long stockBelowMinimumCount,
        List<StatusAmountMetric> salesByStatus,
        List<DayAmountMetric> salesByPeriod,
        List<PaymentMethodMetric> receiptsByPaymentMethod) {}
