package br.com.systemcommerce.pos.report.repository;

import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosMetricSummary;
import br.com.systemcommerce.pos.report.dto.PosPeriodRow;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.report.support.ReportPeriodUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Consultas nativas otimizadas dos relatórios PDV (channel = POS).
 * Filtros e totais calculados no backend.
 */
@Repository
@RequiredArgsConstructor
public class PosReportQueryRepository {

    private static final String EFFECTIVE =
            "('CONFIRMED','PAID','PARTIALLY_PAID')";

    private final EntityManager entityManager;

    public ReportPeriodUtils.InstantRange range(PosReportFilter filter) {
        LocalDate today = ReportPeriodUtils.todayUtc();
        return ReportPeriodUtils.resolve(
                filter.from(),
                filter.to(),
                ReportPeriodUtils.startOfMonth(today),
                ReportPeriodUtils.startOfNextMonth(today));
    }

    public PosMetricSummary metrics(PosReportFilter filter) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(DISTINCT s.id),
                       COALESCE(SUM(s.total_amount), 0),
                       COALESCE(SUM(s.discount_amount), 0),
                       COALESCE(SUM(si.quantity), 0)
                FROM sales s
                LEFT JOIN sale_items si ON si.sale_id = s.id AND si.active = TRUE
                WHERE s.channel = 'POS'
                  AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE));
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        Object[] row = single(sql.toString(), params);
        long count = toLong(row[0]);
        BigDecimal total = toBd(row[1]);
        BigDecimal discount = toBd(row[2]);
        BigDecimal qty = toBd(row[3]);
        BigDecimal avgTicket = avg(total, count);
        BigDecimal avgItems = count == 0
                ? BigDecimal.ZERO
                : qty.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP);

        Double avgService = avgServiceMinutes(filter, r);
        return new PosMetricSummary(
                count, total, avgTicket, qty, discount, avgItems, avgService, r.from(), r.toExclusive());
    }

    public Page<PosAggRow> salesByStore(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT st.id, st.code, st.name, COUNT(s.id), CAST(NULL AS numeric),
                       COALESCE(SUM(s.total_amount),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM sales s
                JOIN stores st ON st.id = s.store_id
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE),
                " GROUP BY st.id, st.code, st.name ORDER BY SUM(s.total_amount) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosAggRow> salesByTerminal(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT t.id, t.code, t.name, COUNT(s.id), CAST(NULL AS numeric),
                       COALESCE(SUM(s.total_amount),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM sales s
                JOIN pos_terminals t ON t.id = s.terminal_id
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE),
                " GROUP BY t.id, t.code, t.name ORDER BY SUM(s.total_amount) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosAggRow> salesByOperator(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT u.id, u.login, u.name, COUNT(s.id), CAST(NULL AS numeric),
                       COALESCE(SUM(s.total_amount),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM sales s
                JOIN users u ON u.id = s.seller_id
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE),
                " GROUP BY u.id, u.login, u.name ORDER BY SUM(s.total_amount) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosAggRow> salesBySession(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT cs.id, CAST(cs.id AS text), CONCAT(t.code, ' / ', u.name),
                       COUNT(s.id), CAST(NULL AS numeric),
                       COALESCE(SUM(s.total_amount),0), CAST(NULL AS numeric), cs.status
                FROM sales s
                JOIN cash_sessions cs ON cs.id = s.cash_session_id
                JOIN pos_terminals t ON t.id = cs.terminal_id
                JOIN users u ON u.id = cs.operator_id
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE),
                " GROUP BY cs.id, t.code, u.name, cs.status ORDER BY SUM(s.total_amount) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosPeriodRow> salesByPeriod(PosReportFilter filter, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT ((s.sale_date AT TIME ZONE 'UTC')::date) AS day,
                       COUNT(s.id), COALESCE(SUM(s.total_amount),0),
                       COALESCE(SUM(si.quantity),0)
                FROM sales s
                LEFT JOIN sale_items si ON si.sale_id = s.id AND si.active = TRUE
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE));
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        sql.append(" GROUP BY day ORDER BY day ");
        return periodPage(sql.toString(), params, pageable, false);
    }

    public Page<PosPeriodRow> salesByHour(PosReportFilter filter, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT ((s.sale_date AT TIME ZONE 'UTC')::date) AS day,
                       EXTRACT(HOUR FROM (s.sale_date AT TIME ZONE 'UTC'))::int AS hr,
                       COUNT(s.id), COALESCE(SUM(s.total_amount),0),
                       COALESCE(SUM(si.quantity),0)
                FROM sales s
                LEFT JOIN sale_items si ON si.sale_id = s.id AND si.active = TRUE
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE));
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        sql.append(" GROUP BY day, hr ORDER BY day, hr ");
        return periodPage(sql.toString(), params, pageable, true);
    }

    public Page<PosAggRow> itemsSold(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT p.id, p.sku, p.name, COUNT(DISTINCT s.id), COALESCE(SUM(si.quantity),0),
                       COALESCE(SUM(si.line_total),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                JOIN products p ON p.id = si.product_id
                WHERE s.channel = 'POS' AND s.status IN %s AND si.active = TRUE
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE),
                " GROUP BY p.id, p.sku, p.name ORDER BY SUM(si.quantity) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosAggRow> topProducts(PosReportFilter filter, Pageable pageable) {
        return itemsSold(filter, pageable);
    }

    public Page<PosAggRow> discounts(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT s.id, s.sale_number, COALESCE(c.name, 'Sem cliente'), 1, CAST(NULL AS numeric),
                       s.total_amount, s.discount_amount, s.status
                FROM sales s
                LEFT JOIN customers c ON c.id = s.customer_id
                WHERE s.channel = 'POS'
                  AND s.discount_amount > 0
                  AND s.sale_date >= :from AND s.sale_date < :to
                """,
                " ORDER BY s.discount_amount DESC ",
                filter,
                pageable,
                false);
    }

    public Page<PosAggRow> discountsByOperator(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT u.id, u.login, u.name, COUNT(s.id), CAST(NULL AS numeric),
                       COALESCE(SUM(s.total_amount),0), COALESCE(SUM(s.discount_amount),0), CAST(NULL AS text)
                FROM sales s
                JOIN users u ON u.id = s.seller_id
                WHERE s.channel = 'POS'
                  AND s.discount_amount > 0
                  AND s.sale_date >= :from AND s.sale_date < :to
                """,
                " GROUP BY u.id, u.login, u.name ORDER BY SUM(s.discount_amount) DESC ",
                filter,
                pageable,
                false);
    }

    public Page<PosAggRow> cancellations(PosReportFilter filter, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT sc.id, s.sale_number, COALESCE(c.name, sc.reason), 1, CAST(NULL AS numeric),
                       s.total_amount, CAST(NULL AS numeric), sc.status
                FROM sale_cancellations sc
                JOIN sales s ON s.id = sc.sale_id
                LEFT JOIN customers c ON c.id = s.customer_id
                WHERE s.channel = 'POS'
                  AND sc.requested_at >= :from AND sc.requested_at < :to
                """);
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        sql.append(" ORDER BY sc.requested_at DESC ");
        return aggPageRaw(sql.toString(), params, pageable, false);
    }

    public Page<PosAggRow> cancelledItems(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT p.id, p.sku, p.name, COUNT(si.id), COALESCE(SUM(si.quantity),0),
                       COALESCE(SUM(si.line_total),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                JOIN products p ON p.id = si.product_id
                WHERE s.channel = 'POS' AND s.status = 'CANCELLED'
                  AND s.sale_date >= :from AND s.sale_date < :to
                """,
                " GROUP BY p.id, p.sku, p.name ORDER BY SUM(si.quantity) DESC ",
                filter,
                pageable,
                true);
    }

    public Page<PosAggRow> suspendedSales(PosReportFilter filter, Pageable pageable) {
        return aggPage(
                """
                SELECT s.id, s.sale_number, COALESCE(c.name, 'Sem cliente'), 1, CAST(NULL AS numeric),
                       s.total_amount, CAST(NULL AS numeric), CAST(s.suspended_at AS text)
                FROM sales s
                LEFT JOIN customers c ON c.id = s.customer_id
                WHERE s.channel = 'POS' AND s.status = 'SUSPENDED'
                  AND s.suspended_at >= :from AND s.suspended_at < :to
                """,
                " ORDER BY s.suspended_at DESC ",
                filter,
                pageable,
                false);
    }

    public Page<PosAggRow> paymentsByMethod(PosReportFilter filter, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT CAST(NULL AS uuid), p.method, p.method, COUNT(p.id), CAST(NULL AS numeric),
                       COALESCE(SUM(p.amount),0), CAST(NULL AS numeric), CAST(NULL AS text)
                FROM payments p
                JOIN sales s ON s.id = p.sale_id
                WHERE s.channel = 'POS'
                  AND p.status = 'CONFIRMED'
                  AND p.paid_at IS NOT NULL
                  AND p.paid_at >= :from AND p.paid_at < :to
                """);
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        if (filter.paymentMethod() != null) {
            sql.append(" AND p.method = :payMethod ");
            params.put("payMethod", filter.paymentMethod().name());
        }
        sql.append(" GROUP BY p.method ORDER BY SUM(p.amount) DESC ");
        return aggPageRaw(sql.toString(), params, pageable, true);
    }

    public Page<PosAggRow> cashMovements(PosReportFilter filter, String movementType, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT cm.id, cm.type, COALESCE(cm.description, cm.type), 1, CAST(NULL AS numeric),
                       cm.amount, CAST(NULL AS numeric), cs.status
                FROM cash_movements cm
                JOIN cash_sessions cs ON cs.id = cm.cash_session_id
                WHERE cm.type = :movType
                  AND cm.occurred_at >= :from AND cm.occurred_at < :to
                """);
        Map<String, Object> params = baseParams(filter, r);
        params.put("movType", movementType);
        if (filter.storeId() != null) {
            sql.append(" AND cs.store_id = :storeId ");
        }
        if (filter.terminalId() != null) {
            sql.append(" AND cs.terminal_id = :terminalId ");
        }
        if (filter.operatorId() != null) {
            sql.append(" AND cs.operator_id = :operatorId ");
        }
        if (filter.cashSessionId() != null) {
            sql.append(" AND cs.id = :cashSessionId ");
        }
        sql.append(" ORDER BY cm.created_at DESC ");
        return aggPageRaw(sql.toString(), params, pageable, false);
    }

    public Page<PosAggRow> cashDifferences(PosReportFilter filter, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT cs.id, CAST(cs.id AS text), CONCAT(t.code, ' / ', u.name), 1, CAST(NULL AS numeric),
                       COALESCE(cs.difference_amount,0), CAST(NULL AS numeric), cs.status
                FROM cash_sessions cs
                JOIN pos_terminals t ON t.id = cs.terminal_id
                JOIN users u ON u.id = cs.operator_id
                WHERE cs.status = 'CLOSED'
                  AND cs.difference_amount IS NOT NULL
                  AND cs.difference_amount <> 0
                  AND cs.closed_at >= :from AND cs.closed_at < :to
                """);
        Map<String, Object> params = baseParams(filter, r);
        if (filter.storeId() != null) {
            sql.append(" AND cs.store_id = :storeId ");
        }
        if (filter.terminalId() != null) {
            sql.append(" AND cs.terminal_id = :terminalId ");
        }
        if (filter.operatorId() != null) {
            sql.append(" AND cs.operator_id = :operatorId ");
        }
        sql.append(" ORDER BY ABS(cs.difference_amount) DESC ");
        return aggPageRaw(sql.toString(), params, pageable, false);
    }

    public Page<PosAggRow> sessionsByStatus(PosReportFilter filter, String status, Pageable pageable) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(
                """
                SELECT cs.id, t.code, u.name, 1, CAST(NULL AS numeric),
                       COALESCE(cs.expected_amount, cs.opening_amount, 0), CAST(NULL AS numeric), cs.status
                FROM cash_sessions cs
                JOIN pos_terminals t ON t.id = cs.terminal_id
                JOIN users u ON u.id = cs.operator_id
                WHERE cs.status = :sessStatus
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("sessStatus", status);
        if ("OPEN".equals(status) || "CLOSING".equals(status)) {
            // abertas: sem filtro de período obrigatório no closed_at
            params.put("from", r.from());
            params.put("to", r.toExclusive());
            sql.append(" AND cs.opened_at < :to ");
        } else {
            params.put("from", r.from());
            params.put("to", r.toExclusive());
            sql.append(" AND cs.closed_at >= :from AND cs.closed_at < :to ");
        }
        if (filter.storeId() != null) {
            sql.append(" AND cs.store_id = :storeId ");
            params.put("storeId", filter.storeId());
        }
        if (filter.terminalId() != null) {
            sql.append(" AND cs.terminal_id = :terminalId ");
            params.put("terminalId", filter.terminalId());
        }
        if (filter.operatorId() != null) {
            sql.append(" AND cs.operator_id = :operatorId ");
            params.put("operatorId", filter.operatorId());
        }
        sql.append(" ORDER BY cs.opened_at DESC ");
        return aggPageRaw(sql.toString(), params, pageable, false);
    }

    public long countOpenSessions(UUID storeId, UUID terminalId) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*) FROM cash_sessions cs
                WHERE cs.status IN ('OPEN','CLOSING')
                """);
        Map<String, Object> params = new HashMap<>();
        if (storeId != null) {
            sql.append(" AND cs.store_id = :storeId ");
            params.put("storeId", storeId);
        }
        if (terminalId != null) {
            sql.append(" AND cs.terminal_id = :terminalId ");
            params.put("terminalId", terminalId);
        }
        return toLong(single(sql.toString(), params)[0]);
    }

    public long countInProgressSales(UUID storeId, UUID terminalId) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*) FROM sales s
                WHERE s.channel = 'POS' AND s.status IN ('DRAFT','SUSPENDED')
                """);
        Map<String, Object> params = new HashMap<>();
        if (storeId != null) {
            sql.append(" AND s.store_id = :storeId ");
            params.put("storeId", storeId);
        }
        if (terminalId != null) {
            sql.append(" AND s.terminal_id = :terminalId ");
            params.put("terminalId", terminalId);
        }
        return toLong(single(sql.toString(), params)[0]);
    }

    public long countCancellationsToday(Instant from, Instant to, UUID storeId, UUID terminalId) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*) FROM sale_cancellations sc
                JOIN sales s ON s.id = sc.sale_id
                WHERE s.channel = 'POS'
                  AND sc.requested_at >= :from AND sc.requested_at < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        if (storeId != null) {
            sql.append(" AND s.store_id = :storeId ");
            params.put("storeId", storeId);
        }
        if (terminalId != null) {
            sql.append(" AND s.terminal_id = :terminalId ");
            params.put("terminalId", terminalId);
        }
        return toLong(single(sql.toString(), params)[0]);
    }

    public PosDashboardResponseMoney cashDiffToday(Instant from, Instant to, UUID storeId, UUID terminalId) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(ABS(cs.difference_amount)),0), COUNT(*)
                FROM cash_sessions cs
                WHERE cs.status = 'CLOSED'
                  AND cs.difference_amount IS NOT NULL AND cs.difference_amount <> 0
                  AND cs.closed_at >= :from AND cs.closed_at < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        if (storeId != null) {
            sql.append(" AND cs.store_id = :storeId ");
            params.put("storeId", storeId);
        }
        if (terminalId != null) {
            sql.append(" AND cs.terminal_id = :terminalId ");
            params.put("terminalId", terminalId);
        }
        Object[] row = single(sql.toString(), params);
        return new PosDashboardResponseMoney(toBd(row[0]), toLong(row[1]));
    }

    public record PosDashboardResponseMoney(BigDecimal amount, long count) {}

    private Double avgServiceMinutes(PosReportFilter filter, ReportPeriodUtils.InstantRange r) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT AVG(EXTRACT(EPOCH FROM (s.updated_at - s.created_at)) / 60.0)
                FROM sales s
                WHERE s.channel = 'POS' AND s.status IN %s
                  AND s.sale_date >= :from AND s.sale_date < :to
                """
                        .formatted(EFFECTIVE));
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        Object[] row = single(sql.toString(), params);
        if (row[0] == null) {
            return null;
        }
        return ((Number) row[0]).doubleValue();
    }

    private Page<PosAggRow> aggPage(
            String selectFromWhere, String groupOrder, PosReportFilter filter, Pageable pageable, boolean computeAvg) {
        var r = range(filter);
        StringBuilder sql = new StringBuilder(selectFromWhere);
        Map<String, Object> params = baseParams(filter, r);
        appendSaleFilters(sql, params, filter, "s");
        if (filter.productId() != null && selectFromWhere.contains("sale_items")) {
            sql.append(" AND si.product_id = :productId ");
            params.put("productId", filter.productId());
        } else if (filter.productId() != null) {
            // ignore on non-item queries
        }
        sql.append(groupOrder);
        return aggPageRaw(sql.toString(), params, pageable, computeAvg);
    }

    @SuppressWarnings("unchecked")
    private Page<PosAggRow> aggPageRaw(
            String sql, Map<String, Object> params, Pageable pageable, boolean computeAvg) {
        Query countQ = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + sql + ") q");
        bind(countQ, params);
        long total = toLong(countQ.getSingleResult());

        Query q = entityManager.createNativeQuery(sql);
        bind(q, params);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<Object[]> rows = q.getResultList();
        List<PosAggRow> content = new ArrayList<>();
        for (Object[] row : rows) {
            long count = toLong(row[3]);
            BigDecimal totalAmount = toBd(row[5]);
            BigDecimal avgTicket = computeAvg ? avg(totalAmount, count) : toBd(row[6]);
            BigDecimal discount = row.length > 6 ? toBd(row[6]) : null;
            if (computeAvg) {
                discount = null;
            }
            // For discounts queries row[6] is discount; for computeAvg we set average in averageTicket
            if (!computeAvg && row.length > 6) {
                avgTicket = toBd(row[6]); // may be discount_amount reused - fix mapping below
            }
            UUID id = row[0] instanceof UUID u ? u : null;
            String code = row[1] != null ? String.valueOf(row[1]) : null;
            String name = row[2] != null ? String.valueOf(row[2]) : null;
            BigDecimal qty = toBd(row[4]);
            String extra = row.length > 7 && row[7] != null ? String.valueOf(row[7]) : null;

            if (computeAvg) {
                content.add(new PosAggRow(id, code, name, count, qty, totalAmount, avg(totalAmount, count), null, extra));
            } else {
                // discount reports: col6 = discount or average field
                content.add(new PosAggRow(id, code, name, count, qty, totalAmount, null, toBd(row[6]), extra));
            }
        }
        return new PageImpl<>(content, pageable, total);
    }

    @SuppressWarnings("unchecked")
    private Page<PosPeriodRow> periodPage(String sql, Map<String, Object> params, Pageable pageable, boolean withHour) {
        Query countQ = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + sql + ") q");
        bind(countQ, params);
        long total = toLong(countQ.getSingleResult());
        Query q = entityManager.createNativeQuery(sql);
        bind(q, params);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<Object[]> rows = q.getResultList();
        List<PosPeriodRow> content = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            if (withHour) {
                int hour = ((Number) row[1]).intValue();
                long count = toLong(row[2]);
                BigDecimal totalAmt = toBd(row[3]);
                BigDecimal qty = toBd(row[4]);
                content.add(new PosPeriodRow(day, hour, count, totalAmt, avg(totalAmt, count), qty));
            } else {
                long count = toLong(row[1]);
                BigDecimal totalAmt = toBd(row[2]);
                BigDecimal qty = toBd(row[3]);
                content.add(new PosPeriodRow(day, null, count, totalAmt, avg(totalAmt, count), qty));
            }
        }
        return new PageImpl<>(content, pageable, total);
    }

    private Map<String, Object> baseParams(PosReportFilter filter, ReportPeriodUtils.InstantRange r) {
        Map<String, Object> params = new HashMap<>();
        params.put("from", r.from());
        params.put("to", r.toExclusive());
        if (filter.storeId() != null) {
            params.put("storeId", filter.storeId());
        }
        if (filter.terminalId() != null) {
            params.put("terminalId", filter.terminalId());
        }
        if (filter.operatorId() != null) {
            params.put("operatorId", filter.operatorId());
        }
        if (filter.cashSessionId() != null) {
            params.put("cashSessionId", filter.cashSessionId());
        }
        if (filter.status() != null) {
            params.put("status", filter.status().name());
        }
        if (filter.customerId() != null) {
            params.put("customerId", filter.customerId());
        }
        if (filter.productId() != null) {
            params.put("productId", filter.productId());
        }
        if (filter.paymentMethod() != null) {
            params.put("payMethod", filter.paymentMethod().name());
        }
        return params;
    }

    private void appendSaleFilters(
            StringBuilder sql, Map<String, Object> params, PosReportFilter filter, String alias) {
        if (filter.storeId() != null) {
            sql.append(" AND ").append(alias).append(".store_id = :storeId ");
        }
        if (filter.terminalId() != null) {
            sql.append(" AND ").append(alias).append(".terminal_id = :terminalId ");
        }
        if (filter.operatorId() != null) {
            sql.append(" AND ").append(alias).append(".seller_id = :operatorId ");
        }
        if (filter.cashSessionId() != null) {
            sql.append(" AND ").append(alias).append(".cash_session_id = :cashSessionId ");
        }
        if (filter.status() != null) {
            sql.append(" AND ").append(alias).append(".status = :status ");
        }
        if (filter.customerId() != null) {
            sql.append(" AND ").append(alias).append(".customer_id = :customerId ");
        }
    }

    private Object[] single(String sql, Map<String, Object> params) {
        Query q = entityManager.createNativeQuery(sql);
        bind(q, params);
        Object result = q.getSingleResult();
        if (result instanceof Object[] arr) {
            return arr;
        }
        return new Object[] {result};
    }

    private void bind(Query q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            try {
                q.setParameter(e.getKey(), e.getValue());
            } catch (IllegalArgumentException ignored) {
                // parameter not present in this query
            }
        }
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        return ((Number) o).longValue();
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(o.toString());
    }

    private static BigDecimal avg(BigDecimal total, long count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof Date d) {
            return d.toLocalDate();
        }
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }
}
