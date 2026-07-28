package br.com.systemcommerce.dashboard.executive.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas analíticas otimizadas para dashboard executivo (Prompt 87). */
@Repository
public class ExecutiveAnalyticsRepository {

    private final JdbcTemplate jdbc;

    public ExecutiveAnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SalesAgg salesAgg(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COALESCE(SUM(s.total_amount), 0), COUNT(*),
                       COALESCE(SUM(si.quantity * COALESCE(p.cost_price, 0)), 0)
                FROM sales s
                JOIN sale_items si ON si.sale_id = s.id
                JOIN products p ON p.id = si.product_id
                WHERE s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                  AND s.sale_date >= ? AND s.sale_date < ?
                  AND (?::uuid IS NULL OR s.store_id = ?::uuid)
                  AND (? = false OR s.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) ->
                            new SalesAgg(rs.getBigDecimal(1), rs.getLong(2), rs.getBigDecimal(3)),
                    from,
                    to,
                    storeId,
                    storeId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new SalesAgg(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        }
    }

    public long countSalesOrders(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COUNT(*) FROM sales_orders so
                WHERE so.created_at >= ? AND so.created_at < ?
                  AND so.status NOT IN ('CANCELLED')
                  AND (?::uuid IS NULL OR so.store_id = ?::uuid)
                  AND (? = false OR so.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        Long c = jdbc.queryForObject(
                sql, Long.class, from, to, storeId, storeId, restrictStores, ids);
        return c != null ? c : 0L;
    }

    public FinancialAgg financialAgg(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COALESCE(SUM(p.amount), 0), COUNT(*)
                FROM payments p
                JOIN sales s ON s.id = p.sale_id
                WHERE p.status = 'CONFIRMED' AND p.paid_at IS NOT NULL
                  AND p.paid_at >= ? AND p.paid_at < ?
                  AND (?::uuid IS NULL OR s.store_id = ?::uuid)
                  AND (? = false OR s.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new FinancialAgg(rs.getBigDecimal(1), rs.getLong(2)),
                    from,
                    to,
                    storeId,
                    storeId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new FinancialAgg(BigDecimal.ZERO, 0L);
        }
    }

    public InventoryAgg inventoryAgg(
            UUID storeId, UUID warehouseId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COALESCE(SUM(i.quantity * COALESCE(p.cost_price, 0)), 0),
                       COALESCE(SUM(i.quantity), 0),
                       COALESCE(SUM(GREATEST(i.quantity - i.quantity_reserved - i.quantity_blocked, 0)), 0),
                       COUNT(*) FILTER (WHERE i.quantity < COALESCE(i.minimum_quantity, p.min_stock, 0)),
                       COUNT(*) FILTER (WHERE GREATEST(i.quantity - i.quantity_reserved - i.quantity_blocked, 0) <= 0
                                        AND COALESCE(i.minimum_quantity, p.min_stock, 0) > 0)
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                JOIN warehouses w ON w.id = i.warehouse_id
                WHERE (?::uuid IS NULL OR i.store_id = ?::uuid)
                  AND (?::uuid IS NULL OR i.warehouse_id = ?::uuid)
                  AND (? = false OR i.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) ->
                            new InventoryAgg(
                                    rs.getBigDecimal(1),
                                    rs.getBigDecimal(2),
                                    rs.getBigDecimal(3),
                                    rs.getLong(4),
                                    rs.getLong(5)),
                    storeId,
                    storeId,
                    warehouseId,
                    warehouseId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new InventoryAgg(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
        }
    }

    public PurchaseAgg purchaseAgg(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COALESCE(SUM(po.total_amount), 0), COUNT(*),
                       COUNT(*) FILTER (WHERE po.status IN ('APPROVED','SENT','SENT_TO_SUPPLIER','CONFIRMED_BY_SUPPLIER','PARTIAL','PARTIALLY_RECEIVED'))
                FROM purchase_orders po
                WHERE po.created_at >= ? AND po.created_at < ?
                  AND po.status NOT IN ('CANCELLED','REJECTED','DRAFT')
                  AND (?::uuid IS NULL OR po.destination_store_id = ?::uuid)
                  AND (? = false OR po.destination_store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new PurchaseAgg(rs.getBigDecimal(1), rs.getLong(2), rs.getLong(3)),
                    from,
                    to,
                    storeId,
                    storeId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new PurchaseAgg(BigDecimal.ZERO, 0L, 0L);
        }
    }

    public CashAgg cashAgg(UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COUNT(*) FILTER (WHERE cs.status = 'OPEN'),
                       COALESCE(SUM(ABS(cs.difference_amount)), 0)
                FROM cash_sessions cs
                WHERE cs.closed_at IS NULL OR cs.closed_at >= (NOW() AT TIME ZONE 'UTC') - INTERVAL '30 days'
                  AND (?::uuid IS NULL OR cs.store_id = ?::uuid)
                  AND (? = false OR cs.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new CashAgg(rs.getLong(1), rs.getBigDecimal(2)),
                    storeId,
                    storeId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new CashAgg(0L, BigDecimal.ZERO);
        }
    }

    public QuoteAgg quoteAgg(UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE q.status IN ('CONVERTED','PARTIALLY_CONVERTED'))
                FROM quotes q
                WHERE q.created_at >= (NOW() AT TIME ZONE 'UTC') - INTERVAL '90 days'
                  AND (?::uuid IS NULL OR q.store_id = ?::uuid)
                  AND (? = false OR q.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new QuoteAgg(rs.getLong(1), rs.getLong(2)),
                    storeId,
                    storeId,
                    restrictStores,
                    ids);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new QuoteAgg(0L, 0L);
        }
    }

    public long newCustomers(Instant from, Instant to) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE created_at >= ? AND created_at < ?",
                Long.class,
                from,
                to);
        return c != null ? c : 0L;
    }

    public ChannelAgg channelAgg(Instant from, Instant to, UUID storeId) {
        String sql =
                """
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE co.status = 'CONVERTED'),
                       COALESCE(SUM(co.total_amount), 0)
                FROM channel_orders co
                JOIN marketplace_accounts ma ON ma.id = co.marketplace_account_id
                WHERE co.received_at >= ? AND co.received_at < ?
                  AND (?::uuid IS NULL OR ma.store_id = ?::uuid)
                """;
        try {
            return jdbc.queryForObject(
                    sql,
                    (rs, rowNum) -> new ChannelAgg(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)),
                    from,
                    to,
                    storeId,
                    storeId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return new ChannelAgg(0L, 0L, BigDecimal.ZERO);
        }
    }

    public List<SeriesRow> revenueByDay(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT (s.sale_date AT TIME ZONE 'UTC')::date::text,
                       COALESCE(SUM(s.total_amount), 0), COUNT(*)
                FROM sales s
                WHERE s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                  AND s.sale_date >= ? AND s.sale_date < ?
                  AND (?::uuid IS NULL OR s.store_id = ?::uuid)
                  AND (? = false OR s.store_id = ANY(?))
                GROUP BY 1 ORDER BY 1
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        return jdbc.query(
                sql,
                (rs, rowNum) -> new SeriesRow(rs.getString(1), rs.getBigDecimal(2), rs.getLong(3)),
                from,
                to,
                storeId,
                storeId,
                restrictStores,
                ids);
    }

    public BigDecimal avgDailySales(
            Instant from, Instant to, UUID storeId, Collection<UUID> allowedStoreIds, boolean restrictStores) {
        String sql =
                """
                SELECT COALESCE(SUM(si.quantity), 0) / GREATEST(EXTRACT(EPOCH FROM (?::timestamptz - ?::timestamptz)) / 86400, 1)
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                WHERE s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                  AND s.sale_date >= ? AND s.sale_date < ?
                  AND (?::uuid IS NULL OR s.store_id = ?::uuid)
                  AND (? = false OR s.store_id = ANY(?))
                """;
        UUID[] ids = allowedStoreIds != null ? allowedStoreIds.toArray(UUID[]::new) : new UUID[0];
        BigDecimal v = jdbc.queryForObject(
                sql, BigDecimal.class, to, from, from, to, storeId, storeId, restrictStores, ids);
        return v != null ? v : BigDecimal.ZERO;
    }

    public record SalesAgg(BigDecimal revenue, long orderCount, BigDecimal costAmount) {}

    public record FinancialAgg(BigDecimal receipts, long count) {}

    public record InventoryAgg(
            BigDecimal stockValue,
            BigDecimal onHand,
            BigDecimal available,
            long belowMin,
            long stockout) {}

    public record PurchaseAgg(BigDecimal amount, long count, long openCount) {}

    public record CashAgg(long openSessions, BigDecimal differenceTotal) {}

    public record QuoteAgg(long total, long converted) {}

    public record ChannelAgg(long received, long converted, BigDecimal revenue) {}

    public record SeriesRow(String label, BigDecimal amount, long count) {}
}
