package br.com.systemcommerce.bi.controller;

import br.com.systemcommerce.bi.dto.BiRefreshLogResponse;
import br.com.systemcommerce.bi.service.BiRefreshService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "BI Analytics", description = "Camada analítica gerencial (Prompt 88)")
public class BiAnalyticsController {

    private final BiRefreshService biRefreshService;
    private final JdbcTemplate jdbc;

    @GetMapping("/refresh-log")
    @PreAuthorize("hasAuthority('BI_ANALYTICS_READ')")
    public ApiResponse<List<BiRefreshLogResponse>> refreshLog() {
        return ApiResponse.of(biRefreshService.recentLogs().stream()
                .map(l -> new BiRefreshLogResponse(
                        l.getId(),
                        l.getObjectName(),
                        l.getRefreshType(),
                        l.getStartedAt(),
                        l.getFinishedAt(),
                        l.getStatus(),
                        l.getRowsAffected(),
                        l.getErrorMessage()))
                .toList());
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('BI_ANALYTICS_MANAGE')")
    public ApiResponse<List<BiRefreshLogResponse>> refresh(
            @RequestParam(required = false) String objectName) {
        var logs = objectName != null && !objectName.isBlank()
                ? List.of(biRefreshService.refreshObject(objectName.trim()))
                : biRefreshService.refreshAll();
        return ApiResponse.of(logs.stream()
                .map(l -> new BiRefreshLogResponse(
                        l.getId(),
                        l.getObjectName(),
                        l.getRefreshType(),
                        l.getStartedAt(),
                        l.getFinishedAt(),
                        l.getStatus(),
                        l.getRowsAffected(),
                        l.getErrorMessage()))
                .toList());
    }

    @GetMapping("/sales-daily")
    @PreAuthorize("hasAuthority('BI_ANALYTICS_READ')")
    public ApiResponse<List<java.util.Map<String, Object>>> salesDaily(
            @RequestParam(required = false) java.util.UUID organizationId,
            @RequestParam(required = false) java.util.UUID storeId,
            @RequestParam(defaultValue = "90") int days) {
        String sql =
                """
                SELECT organization_id, store_id, sale_day, order_count, revenue, cost_amount
                FROM bi_fact_sales_daily
                WHERE sale_day >= (CURRENT_DATE - (? * INTERVAL '1 day'))
                  AND (?::uuid IS NULL OR organization_id = ?::uuid)
                  AND (?::uuid IS NULL OR store_id = ?::uuid)
                ORDER BY sale_day DESC
                LIMIT 500
                """;
        return ApiResponse.of(jdbc.query(
                sql,
                (rs, rowNum) -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("organizationId", rs.getObject("organization_id"));
                    m.put("storeId", rs.getObject("store_id"));
                    m.put("saleDay", rs.getObject("sale_day"));
                    m.put("orderCount", rs.getLong("order_count"));
                    m.put("revenue", rs.getBigDecimal("revenue"));
                    m.put("costAmount", rs.getBigDecimal("cost_amount"));
                    return m;
                },
                days,
                organizationId,
                organizationId,
                storeId,
                storeId));
    }

    @GetMapping("/inventory-snapshot")
    @PreAuthorize("hasAuthority('BI_ANALYTICS_READ')")
    public ApiResponse<List<java.util.Map<String, Object>>> inventorySnapshot(
            @RequestParam(required = false) java.util.UUID storeId,
            @RequestParam(required = false) java.util.UUID warehouseId,
            @RequestParam(defaultValue = "200") int limit) {
        String sql =
                """
                SELECT organization_id, store_id, warehouse_id, product_id,
                       on_hand, available, stock_value, min_qty, reorder_point
                FROM bi_fact_inventory_snapshot
                WHERE (?::uuid IS NULL OR store_id = ?::uuid)
                  AND (?::uuid IS NULL OR warehouse_id = ?::uuid)
                ORDER BY stock_value DESC NULLS LAST
                LIMIT ?
                """;
        return ApiResponse.of(jdbc.query(
                sql,
                (rs, rowNum) -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("organizationId", rs.getObject("organization_id"));
                    m.put("storeId", rs.getObject("store_id"));
                    m.put("warehouseId", rs.getObject("warehouse_id"));
                    m.put("productId", rs.getObject("product_id"));
                    m.put("onHand", rs.getBigDecimal("on_hand"));
                    m.put("available", rs.getBigDecimal("available"));
                    m.put("stockValue", rs.getBigDecimal("stock_value"));
                    m.put("minQty", rs.getBigDecimal("min_qty"));
                    m.put("reorderPoint", rs.getBigDecimal("reorder_point"));
                    return m;
                },
                storeId,
                storeId,
                warehouseId,
                warehouseId,
                limit));
    }

    @GetMapping("/purchases-daily")
    @PreAuthorize("hasAuthority('BI_ANALYTICS_READ')")
    public ApiResponse<List<java.util.Map<String, Object>>> purchasesDaily(
            @RequestParam(required = false) java.util.UUID organizationId,
            @RequestParam(required = false) java.util.UUID storeId,
            @RequestParam(defaultValue = "90") int days) {
        String sql =
                """
                SELECT organization_id, store_id, purchase_day, po_count, purchase_amount
                FROM bi_fact_purchases_daily
                WHERE purchase_day >= (CURRENT_DATE - (? * INTERVAL '1 day'))
                  AND (?::uuid IS NULL OR organization_id = ?::uuid)
                  AND (?::uuid IS NULL OR store_id = ?::uuid)
                ORDER BY purchase_day DESC
                LIMIT 500
                """;
        return ApiResponse.of(jdbc.query(
                sql,
                (rs, rowNum) -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("organizationId", rs.getObject("organization_id"));
                    m.put("storeId", rs.getObject("store_id"));
                    m.put("purchaseDay", rs.getObject("purchase_day"));
                    m.put("poCount", rs.getLong("po_count"));
                    m.put("purchaseAmount", rs.getBigDecimal("purchase_amount"));
                    return m;
                },
                days,
                organizationId,
                organizationId,
                storeId,
                storeId));
    }
}
