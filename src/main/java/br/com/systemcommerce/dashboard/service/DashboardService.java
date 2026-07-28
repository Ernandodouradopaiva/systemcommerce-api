package br.com.systemcommerce.dashboard.service;

import br.com.systemcommerce.dashboard.dto.DashboardSummaryResponse;
import br.com.systemcommerce.dashboard.dto.MoneyCountMetric;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.report.repository.ReportQueryRepository;
import br.com.systemcommerce.report.support.ReportPeriodUtils;
import br.com.systemcommerce.report.support.ReportRowMapper;
import br.com.systemcommerce.report.support.ReportScope;
import br.com.systemcommerce.report.support.ReportStoreAccessSupport;
import br.com.systemcommerce.report.support.ReportStoreFilter;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores agregados do dashboard ERP.
 * <p>
 * Aceita {@code storeId} e {@code scope} ({@link ReportScope#STORE STORE},
 * {@link ReportScope#MULTI MULTI}, {@link ReportScope#GLOBAL GLOBAL}). Usuários sem escopo global
 * consultam apenas lojas acessíveis via {@link StoreAuthorizationEvaluator}.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final List<Sale.SaleStatus> EFFECTIVE_SALE_STATUSES = List.of(
            Sale.SaleStatus.CONFIRMED, Sale.SaleStatus.PAID, Sale.SaleStatus.PARTIALLY_PAID);

    private final ReportQueryRepository reportQueryRepository;
    private final ReportStoreAccessSupport reportStoreAccessSupport;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(
            Integer topLimit, Integer periodDays, UUID storeId, ReportScope scope) {
        int top = topLimit == null || topLimit < 1 ? 5 : Math.min(topLimit, 20);
        int days = periodDays == null || periodDays < 1 ? 14 : Math.min(periodDays, 90);

        ReportStoreFilter filter = reportStoreAccessSupport.resolveDashboardFilter(storeId, scope);
        UUID effectiveStoreId = filter.storeId();
        var allowedStoreIds = filter.allowedStoreIds();
        var nativeAllowed = filter.nativeAllowedStores();

        LocalDate today = ReportPeriodUtils.todayUtc();
        var day = ReportPeriodUtils.dayRange(today);
        var month = ReportPeriodUtils.monthRange(today);
        Instant periodFrom = ReportPeriodUtils.startOfDay(today.minusDays(days - 1L));
        Instant periodTo = day.toExclusive();

        if (filter.isEmpty()) {
            return emptySummary(day.from(), day.toExclusive(), month.from(), month.toExclusive());
        }

        MoneyCountMetric salesToday =
                ReportRowMapper.toMoneyCount(reportQueryRepository.sumAndCountSales(
                        EFFECTIVE_SALE_STATUSES,
                        day.from(),
                        day.toExclusive(),
                        effectiveStoreId,
                        allowedStoreIds));
        MoneyCountMetric salesMonth =
                ReportRowMapper.toMoneyCount(reportQueryRepository.sumAndCountSales(
                        EFFECTIVE_SALE_STATUSES,
                        month.from(),
                        month.toExclusive(),
                        effectiveStoreId,
                        allowedStoreIds));

        return new DashboardSummaryResponse(
                Instant.now(),
                day.from(),
                day.toExclusive(),
                month.from(),
                month.toExclusive(),
                salesToday,
                salesMonth,
                ReportRowMapper.averageTicket(salesMonth),
                ReportRowMapper.toTopProducts(reportQueryRepository.topProducts(
                        EFFECTIVE_SALE_STATUSES,
                        month.from(),
                        month.toExclusive(),
                        effectiveStoreId,
                        allowedStoreIds,
                        PageRequest.of(0, top))),
                ReportRowMapper.toTopCustomers(reportQueryRepository.topCustomers(
                        EFFECTIVE_SALE_STATUSES,
                        month.from(),
                        month.toExclusive(),
                        effectiveStoreId,
                        allowedStoreIds,
                        PageRequest.of(0, top))),
                reportQueryRepository.countStockBelowMinimum(effectiveStoreId, allowedStoreIds),
                ReportRowMapper.toStatusMetrics(reportQueryRepository.countSalesByStatus(
                        month.from(), month.toExclusive(), effectiveStoreId, allowedStoreIds)),
                ReportRowMapper.toDayMetrics(reportQueryRepository.salesByDay(
                        periodFrom,
                        periodTo,
                        effectiveStoreId,
                        nativeAllowed.restrict(),
                        nativeAllowed.ids())),
                ReportRowMapper.toPaymentMethodMetrics(reportQueryRepository.paymentsByMethod(
                        Payment.PaymentStatus.CONFIRMED,
                        month.from(),
                        month.toExclusive(),
                        effectiveStoreId,
                        allowedStoreIds)));
    }

    private static DashboardSummaryResponse emptySummary(
            Instant dayFrom, Instant dayTo, Instant monthFrom, Instant monthTo) {
        MoneyCountMetric zero = new MoneyCountMetric(java.math.BigDecimal.ZERO, 0L);
        return new DashboardSummaryResponse(
                Instant.now(),
                dayFrom,
                dayTo,
                monthFrom,
                monthTo,
                zero,
                zero,
                java.math.BigDecimal.ZERO,
                List.of(),
                List.of(),
                0L,
                List.of(),
                List.of(),
                List.of());
    }
}
