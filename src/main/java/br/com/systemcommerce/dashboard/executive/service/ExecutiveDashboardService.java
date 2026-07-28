package br.com.systemcommerce.dashboard.executive.service;

import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardQuery;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.CashKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.ChannelKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.DrillDownSeriesPoint;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.FinancialKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.InventoryKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.PeopleKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.PurchaseKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse.SalesKpi;
import br.com.systemcommerce.dashboard.executive.dto.ExecutivePerspective;
import br.com.systemcommerce.dashboard.executive.repository.ExecutiveAnalyticsRepository;
import br.com.systemcommerce.dashboard.executive.support.ExecutiveDashboardCache;
import br.com.systemcommerce.pricing.repository.StoreGroupMemberRepository;
import br.com.systemcommerce.report.support.ReportScope;
import br.com.systemcommerce.report.support.ReportStoreAccessSupport;
import br.com.systemcommerce.report.support.ReportStoreFilter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import br.com.systemcommerce.report.support.ReportPeriodUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

@Service
@RequiredArgsConstructor
public class ExecutiveDashboardService {

    private final ExecutiveAnalyticsRepository analyticsRepository;
    private final ReportStoreAccessSupport reportStoreAccessSupport;
    private final ExecutiveDashboardCache cache;
    private final StoreGroupMemberRepository storeGroupMemberRepository;

    @Transactional(readOnly = true)
    public ExecutiveDashboardResponse build(ExecutiveDashboardQuery query) {
        String cacheKey = cacheKey(query);
        return cache.get(cacheKey, ExecutiveDashboardResponse.class)
                .orElseGet(() -> {
                    ExecutiveDashboardResponse response = compute(query);
                    cache.put(cacheKey, response);
                    return response;
                });
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(ExecutiveDashboardQuery query) {
        ExecutiveDashboardResponse r = build(query);
        StringBuilder sb = new StringBuilder();
        sb.append("indicador,valor\n");
        sb.append("faturamento,").append(r.sales().revenue()).append('\n');
        sb.append("pedidos_venda,").append(r.sales().orderCount()).append('\n');
        sb.append("ticket_medio,").append(r.sales().averageTicket()).append('\n');
        sb.append("margem_percent,").append(r.sales().marginPercent()).append('\n');
        sb.append("custo,").append(r.sales().costAmount()).append('\n');
        sb.append("estoque_valor,").append(r.inventory().stockValue()).append('\n');
        sb.append("ruptura,").append(r.inventory().stockoutCount()).append('\n');
        sb.append("cobertura_dias,").append(r.inventory().coverageDays()).append('\n');
        sb.append("compras,").append(r.purchases().purchaseAmount()).append('\n');
        sb.append("recebimentos,").append(r.financial().receiptsConfirmed()).append('\n');
        sb.append("caixas_abertos,").append(r.cash().openSessionsCount()).append('\n');
        sb.append("diferenca_caixa,").append(r.cash().cashDifferenceTotal()).append('\n');
        sb.append("conversao_orcamentos,").append(r.people().quoteConversionRate()).append('\n');
        sb.append("pedidos_canal,").append(r.channels().channelOrdersReceived()).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ExecutiveDashboardResponse compute(ExecutiveDashboardQuery query) {
        ReportStoreFilter filter = resolveFilter(query);
        filter = applyPerspectiveFilter(query, filter);
        if (filter.isEmpty()) {
            return emptyResponse(query);
        }

        Instant from = query.from() != null ? query.from() : defaultPeriodFrom();
        Instant to = query.to() != null ? query.to() : ReportPeriodUtils.dayRange(ReportPeriodUtils.todayUtc()).toExclusive();
        UUID storeId = effectiveStoreId(query, filter);
        UUID warehouseId = query.warehouseId();
        boolean restrict = filter.nativeAllowedStores().restrict();
        var allowed = filter.nativeAllowedStores().ids();

        var salesAgg = analyticsRepository.salesAgg(from, to, storeId, allowed, restrict);
        long salesOrders = analyticsRepository.countSalesOrders(from, to, storeId, allowed, restrict);
        var financial = analyticsRepository.financialAgg(from, to, storeId, allowed, restrict);
        var inventory = analyticsRepository.inventoryAgg(storeId, warehouseId, allowed, restrict);
        var purchases = analyticsRepository.purchaseAgg(from, to, storeId, allowed, restrict);
        var cash = analyticsRepository.cashAgg(storeId, allowed, restrict);
        var quotes = analyticsRepository.quoteAgg(storeId, allowed, restrict);
        var channels = analyticsRepository.channelAgg(from, to, storeId);
        BigDecimal avgDaily = analyticsRepository.avgDailySales(from, to, storeId, allowed, restrict);

        BigDecimal revenue = nullToZero(salesAgg.revenue());
        long orderCount = salesAgg.orderCount();
        BigDecimal cost = nullToZero(salesAgg.costAmount());
        BigDecimal margin = revenue.subtract(cost);
        BigDecimal marginPct = revenue.signum() == 0
                ? BigDecimal.ZERO
                : margin.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP);
        BigDecimal ticket = orderCount == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        BigDecimal available = nullToZero(inventory.available());
        BigDecimal coverage = avgDaily.signum() == 0
                ? BigDecimal.ZERO
                : available.divide(avgDaily, 2, RoundingMode.HALF_UP);
        BigDecimal turnover = inventory.onHand().signum() == 0
                ? BigDecimal.ZERO
                : cost.divide(inventory.onHand(), 4, RoundingMode.HALF_UP);

        BigDecimal quoteRate = quotes.total() == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(quotes.converted())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(quotes.total()), 2, RoundingMode.HALF_UP);

        List<DrillDownSeriesPoint> series = analyticsRepository.revenueByDay(from, to, storeId, allowed, restrict)
                .stream()
                .map(r -> new DrillDownSeriesPoint(r.label(), r.amount(), r.count()))
                .toList();

        return new ExecutiveDashboardResponse(
                Instant.now(),
                query.timezone() != null ? query.timezone() : "UTC",
                query.perspective() != null ? query.perspective() : ExecutivePerspective.PERIOD,
                from,
                to,
                query.organizationId(),
                storeId,
                warehouseId,
                query.channelCode(),
                new SalesKpi(revenue, orderCount, ticket, margin, marginPct, cost, salesOrders),
                new FinancialKpi(nullToZero(financial.receipts()), financial.count(), BigDecimal.ZERO),
                new InventoryKpi(
                        nullToZero(inventory.stockValue()),
                        nullToZero(inventory.onHand()),
                        available,
                        inventory.belowMin(),
                        inventory.stockout(),
                        turnover,
                        coverage,
                        BigDecimal.ZERO),
                new PurchaseKpi(
                        nullToZero(purchases.amount()),
                        purchases.count(),
                        purchases.openCount(),
                        BigDecimal.ZERO),
                new CashKpi(cash.openSessions(), nullToZero(cash.differenceTotal())),
                new PeopleKpi(
                        analyticsRepository.newCustomers(from, to),
                        quoteRate,
                        quotes.total(),
                        quotes.converted(),
                        BigDecimal.ZERO),
                new ChannelKpi(channels.received(), channels.converted(), nullToZero(channels.revenue())),
                series);
    }

    private ReportStoreFilter applyPerspectiveFilter(ExecutiveDashboardQuery query, ReportStoreFilter base) {
        if (query.perspective() == ExecutivePerspective.STORE_GROUP && query.storeGroupId() != null) {
            List<UUID> groupStores = storeGroupMemberRepository.findStoreIdsByStoreGroupId(query.storeGroupId());
            if (groupStores.isEmpty()) {
                return ReportStoreFilter.multi(List.of());
            }
            if (base.allowedStoreIds() == null && base.storeId() == null) {
                return ReportStoreFilter.multi(groupStores);
            }
            Set<UUID> allowed = base.allowedStoreIds() != null
                    ? new HashSet<>(base.allowedStoreIds())
                    : new HashSet<>(List.of(base.storeId()));
            List<UUID> intersection = groupStores.stream().filter(allowed::contains).toList();
            return intersection.isEmpty()
                    ? ReportStoreFilter.multi(List.of())
                    : ReportStoreFilter.multi(intersection);
        }
        return base;
    }

    private ReportStoreFilter resolveFilter(ExecutiveDashboardQuery query) {
        UUID storeId = query.storeId();
        ReportScope scope = switch (query.perspective() != null ? query.perspective() : ExecutivePerspective.STORE) {
            case ORGANIZATION, STORE_GROUP, PERIOD, CHANNEL -> ReportScope.MULTI;
            case STORE -> ReportScope.STORE;
            case WAREHOUSE -> ReportScope.STORE;
        };
        return reportStoreAccessSupport.resolveDashboardFilter(storeId, scope);
    }

    private UUID effectiveStoreId(ExecutiveDashboardQuery query, ReportStoreFilter filter) {
        if (query.warehouseId() != null || query.perspective() == ExecutivePerspective.WAREHOUSE) {
            return query.storeId() != null ? query.storeId() : filter.storeId();
        }
        return filter.storeId();
    }

    private static Instant defaultPeriodFrom() {
        LocalDate today = ReportPeriodUtils.todayUtc();
        return ReportPeriodUtils.startOfDay(today.withDayOfMonth(1));
    }

    private static ExecutiveDashboardResponse emptyResponse(ExecutiveDashboardQuery query) {
        Instant now = Instant.now();
        return new ExecutiveDashboardResponse(
                now,
                "UTC",
                query.perspective(),
                now,
                now,
                query.organizationId(),
                null,
                null,
                null,
                new SalesKpi(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new FinancialKpi(BigDecimal.ZERO, 0, BigDecimal.ZERO),
                new InventoryKpi(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO),
                new PurchaseKpi(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO),
                new CashKpi(0, BigDecimal.ZERO),
                new PeopleKpi(0, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO),
                new ChannelKpi(0, 0, BigDecimal.ZERO),
                List.of());
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String cacheKey(ExecutiveDashboardQuery q) {
        String raw = String.join(
                "|",
                String.valueOf(q.perspective()),
                String.valueOf(q.organizationId()),
                String.valueOf(q.storeGroupId()),
                String.valueOf(q.storeId()),
                String.valueOf(q.warehouseId()),
                String.valueOf(q.channelCode()),
                String.valueOf(q.from()),
                String.valueOf(q.to()));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
