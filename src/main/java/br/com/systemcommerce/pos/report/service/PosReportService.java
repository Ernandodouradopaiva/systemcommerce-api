package br.com.systemcommerce.pos.report.service;

import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosExportFormat;
import br.com.systemcommerce.pos.report.dto.PosMetricSummary;
import br.com.systemcommerce.pos.report.dto.PosPeriodRow;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.pos.report.dto.PosReportType;
import br.com.systemcommerce.pos.report.repository.PosReportQueryRepository;
import br.com.systemcommerce.pos.report.support.PosReportAccessGuard;
import br.com.systemcommerce.pos.report.support.PosReportExporter;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosReportService {

    private static final int CSV_MAX = 10_000;

    private final PosReportQueryRepository queryRepository;
    private final PosReportAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public Page<PosAggRow> aggregate(PosReportType type, PosReportFilter filter, Pageable pageable) {
        accessGuard.assertRead();
        PosReportFilter scoped = accessGuard.enforceStoreScope(filter);
        return switch (type) {
            case SALES_BY_STORE -> queryRepository.salesByStore(scoped, pageable);
            case SALES_BY_TERMINAL -> queryRepository.salesByTerminal(scoped, pageable);
            case SALES_BY_OPERATOR -> queryRepository.salesByOperator(scoped, pageable);
            case SALES_BY_SESSION -> queryRepository.salesBySession(scoped, pageable);
            case ITEMS_SOLD, TOP_PRODUCTS -> queryRepository.itemsSold(scoped, pageable);
            case DISCOUNTS -> queryRepository.discounts(scoped, pageable);
            case DISCOUNTS_BY_OPERATOR -> queryRepository.discountsByOperator(scoped, pageable);
            case CANCELLATIONS -> queryRepository.cancellations(scoped, pageable);
            case CANCELLED_ITEMS -> queryRepository.cancelledItems(scoped, pageable);
            case SUSPENDED_SALES -> queryRepository.suspendedSales(scoped, pageable);
            case PAYMENTS_BY_METHOD -> queryRepository.paymentsByMethod(scoped, pageable);
            case WITHDRAWALS -> queryRepository.cashMovements(scoped, "WITHDRAWAL", pageable);
            case SUPPLIES -> queryRepository.cashMovements(scoped, "SUPPLY", pageable);
            case CASH_DIFFERENCES -> queryRepository.cashDifferences(scoped, pageable);
            case SESSIONS_OPEN -> queryRepository.sessionsByStatus(scoped, "OPEN", pageable);
            case SESSIONS_CLOSED -> queryRepository.sessionsByStatus(scoped, "CLOSED", pageable);
            case AVERAGE_TICKET, AVG_SERVICE_TIME, AVG_ITEMS_PER_SALE -> metricAsPage(scoped, type, pageable);
            case SALES_BY_PERIOD, SALES_BY_HOUR ->
                    throw new BusinessRuleException("Use o endpoint de período/hora para este relatório");
        };
    }

    @Transactional(readOnly = true)
    public Page<PosPeriodRow> byPeriod(PosReportFilter filter, Pageable pageable) {
        accessGuard.assertRead();
        return queryRepository.salesByPeriod(accessGuard.enforceStoreScope(filter), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PosPeriodRow> byHour(PosReportFilter filter, Pageable pageable) {
        accessGuard.assertRead();
        return queryRepository.salesByHour(accessGuard.enforceStoreScope(filter), pageable);
    }

    @Transactional(readOnly = true)
    public PosMetricSummary summary(PosReportFilter filter) {
        accessGuard.assertRead();
        return queryRepository.metrics(accessGuard.enforceStoreScope(filter));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(
            PosReportType type, PosReportFilter filter, PosExportFormat format) {
        accessGuard.assertExport();
        if (format == null) {
            format = PosExportFormat.CSV;
        }
        PosReportFilter scoped = accessGuard.enforceStoreScope(filter);
        Pageable pageable = PageRequest.of(0, CSV_MAX);

        if (type == PosReportType.SALES_BY_PERIOD) {
            return PosReportExporter.exportPeriod(
                    "pdv-vendas-periodo.csv",
                    queryRepository.salesByPeriod(scoped, pageable).getContent(),
                    format);
        }
        if (type == PosReportType.SALES_BY_HOUR) {
            return PosReportExporter.exportPeriod(
                    "pdv-vendas-hora.csv",
                    queryRepository.salesByHour(scoped, pageable).getContent(),
                    format);
        }

        List<PosAggRow> rows = aggregate(type, scoped, pageable).getContent();
        return PosReportExporter.exportAgg(
                "pdv-" + type.name().toLowerCase().replace('_', '-') + ".csv",
                List.of("Id", "Codigo", "Nome", "Qtd", "Quantidade", "Total", "TicketMedio", "Desconto", "Extra"),
                rows,
                format);
    }

    private Page<PosAggRow> metricAsPage(PosReportFilter filter, PosReportType type, Pageable pageable) {
        PosMetricSummary m = queryRepository.metrics(filter);
        PosAggRow row =
                switch (type) {
                    case AVERAGE_TICKET -> new PosAggRow(
                            null,
                            "AVERAGE_TICKET",
                            "Ticket médio",
                            m.saleCount(),
                            m.itemQuantity(),
                            m.totalAmount(),
                            m.averageTicket(),
                            m.discountAmount(),
                            null);
                    case AVG_ITEMS_PER_SALE -> new PosAggRow(
                            null,
                            "AVG_ITEMS_PER_SALE",
                            "Média de itens por venda",
                            m.saleCount(),
                            m.avgItemsPerSale(),
                            m.totalAmount(),
                            m.averageTicket(),
                            null,
                            null);
                    case AVG_SERVICE_TIME -> new PosAggRow(
                            null,
                            "AVG_SERVICE_TIME",
                            "Tempo médio de atendimento (min)",
                            m.saleCount(),
                            null,
                            m.totalAmount(),
                            m.averageTicket(),
                            null,
                            m.avgServiceMinutes() == null
                                    ? null
                                    : String.valueOf(Math.round(m.avgServiceMinutes() * 100.0) / 100.0));
                    default -> throw new BusinessRuleException("Tipo de métrica inválido");
                };
        return new PageImpl<>(List.of(row), pageable, 1);
    }
}
