package br.com.systemcommerce.pos.report.service;

import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosDashboardResponse;
import br.com.systemcommerce.pos.report.dto.PosMetricSummary;
import br.com.systemcommerce.pos.report.dto.PosPeriodRow;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.pos.report.repository.PosReportQueryRepository;
import br.com.systemcommerce.pos.report.support.PosReportAccessGuard;
import br.com.systemcommerce.report.support.ReportPeriodUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosDashboardService {

    private final PosReportQueryRepository queryRepository;
    private final PosReportAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public PosDashboardResponse summary(UUID storeId, UUID terminalId) {
        accessGuard.assertDashboard();
        PosReportFilter filter = new PosReportFilter(
                null, null, storeId, terminalId, null, null, null, null, null, null);
        filter = accessGuard.enforceStoreScope(filter);

        var day = ReportPeriodUtils.dayRange(ReportPeriodUtils.todayUtc());
        PosReportFilter dayFilter = new PosReportFilter(
                day.from(),
                day.toExclusive(),
                filter.storeId(),
                filter.terminalId(),
                null,
                null,
                null,
                null,
                null,
                null);

        PosMetricSummary metrics = queryRepository.metrics(dayFilter);
        List<PosAggRow> payments =
                queryRepository.paymentsByMethod(dayFilter, PageRequest.of(0, 20)).getContent();
        List<PosPeriodRow> byHour =
                queryRepository.salesByHour(dayFilter, PageRequest.of(0, 24)).getContent();

        long inProgress = queryRepository.countInProgressSales(filter.storeId(), filter.terminalId());
        long openSessions = queryRepository.countOpenSessions(filter.storeId(), filter.terminalId());
        long cancellations = queryRepository.countCancellationsToday(
                day.from(), day.toExclusive(), filter.storeId(), filter.terminalId());
        var diffs = queryRepository.cashDiffToday(
                day.from(), day.toExclusive(), filter.storeId(), filter.terminalId());

        Instant now = Instant.now();
        return new PosDashboardResponse(
                now,
                day.from(),
                day.toExclusive(),
                filter.storeId(),
                filter.terminalId(),
                new PosDashboardResponse.MoneyCount(metrics.totalAmount(), metrics.saleCount()),
                new PosDashboardResponse.MoneyCount(
                        payments.stream()
                                .map(PosAggRow::totalAmount)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                        payments.stream().mapToLong(PosAggRow::count).sum()),
                inProgress,
                openSessions,
                metrics.averageTicket(),
                metrics.itemQuantity(),
                cancellations,
                new PosDashboardResponse.MoneyCount(diffs.amount(), diffs.count()),
                byHour,
                payments);
    }
}
