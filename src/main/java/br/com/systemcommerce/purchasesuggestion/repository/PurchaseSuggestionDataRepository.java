package br.com.systemcommerce.purchasesuggestion.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseSuggestionDataRepository {

    private final JdbcTemplate jdbc;

    public PurchaseSuggestionDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record InventoryRow(
            UUID productId,
            String sku,
            String name,
            BigDecimal onHand,
            BigDecimal available,
            BigDecimal inTransit,
            BigDecimal reorderPoint,
            BigDecimal maxStock,
            BigDecimal minStock) {}

    public record SupplierHint(UUID supplierId, Integer leadTimeDays) {}

    public List<InventoryRow> listWarehouseInventory(UUID warehouseId) {
        String sql =
                """
                SELECT p.id, p.sku, p.name,
                       i.quantity,
                       GREATEST(i.quantity - i.quantity_reserved - i.quantity_blocked, 0),
                       i.quantity_in_transit,
                       i.reorder_point,
                       i.maximum_quantity,
                       COALESCE(i.minimum_quantity, p.min_stock, 0)
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                WHERE i.warehouse_id = ?
                  AND p.active = TRUE
                ORDER BY p.sku
                """;
        return jdbc.query(
                sql,
                (rs, rowNum) ->
                        new InventoryRow(
                                rs.getObject("id", UUID.class),
                                rs.getString("sku"),
                                rs.getString("name"),
                                rs.getBigDecimal("quantity"),
                                rs.getBigDecimal("greatest"),
                                rs.getBigDecimal("quantity_in_transit"),
                                rs.getBigDecimal("reorder_point"),
                                rs.getBigDecimal("maximum_quantity"),
                                rs.getBigDecimal("coalesce")),
                warehouseId);
    }

    public BigDecimal avgDailyConsumption(UUID storeId, UUID productId, int lookbackDays) {
        String sql =
                """
                SELECT COALESCE(SUM(si.quantity), 0)
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                WHERE s.store_id = ?
                  AND si.product_id = ?
                  AND s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                  AND s.sale_date >= (NOW() AT TIME ZONE 'UTC' - (? * INTERVAL '1 day'))
                """;
        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, storeId, productId, lookbackDays);
        if (total == null || lookbackDays <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(lookbackDays), 4, RoundingMode.HALF_UP);
    }

    public int consumptionHistoryDays(UUID storeId, UUID productId, int lookbackDays) {
        String sql =
                """
                SELECT COUNT(DISTINCT (s.sale_date AT TIME ZONE 'UTC')::date)
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                WHERE s.store_id = ?
                  AND si.product_id = ?
                  AND s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                  AND s.sale_date >= (NOW() AT TIME ZONE 'UTC' - (? * INTERVAL '1 day'))
                """;
        Integer days = jdbc.queryForObject(sql, Integer.class, storeId, productId, lookbackDays);
        return days != null ? days : 0;
    }

    public BigDecimal openPurchaseOrderQty(UUID storeId, UUID productId) {
        String sql =
                """
                SELECT COALESCE(SUM(poi.quantity_ordered - poi.quantity_received - poi.quantity_cancelled), 0)
                FROM purchase_order_items poi
                JOIN purchase_orders po ON po.id = poi.purchase_order_id
                WHERE po.destination_store_id = ?
                  AND poi.product_id = ?
                  AND po.status IN (
                      'PENDING_APPROVAL', 'APPROVED', 'SENT', 'SENT_TO_SUPPLIER',
                      'CONFIRMED_BY_SUPPLIER', 'PARTIAL', 'PARTIALLY_RECEIVED')
                """;
        BigDecimal qty = jdbc.queryForObject(sql, BigDecimal.class, storeId, productId);
        return qty != null ? qty.max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public Optional<SupplierHint> preferredSupplier(UUID productId) {
        String sql =
                """
                SELECT sp.supplier_id, sp.lead_time_days
                FROM supplier_products sp
                JOIN suppliers sup ON sup.id = sp.supplier_id
                WHERE sp.product_id = ?
                  AND sp.active = TRUE
                  AND sup.active = TRUE
                ORDER BY sp.lead_time_days NULLS LAST, sp.updated_at DESC
                LIMIT 1
                """;
        List<SupplierHint> rows =
                jdbc.query(
                        sql,
                        (rs, rowNum) ->
                                new SupplierHint(
                                        rs.getObject("supplier_id", UUID.class),
                                        (Integer) rs.getObject("lead_time_days")),
                        productId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
