package br.com.systemcommerce.report.repository;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.sale.entity.Sale;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas agregadas/projetadas para dashboard e relatórios.
 * Não devolve entidades completas desnecessariamente.
 * Filtros {@code storeId} / {@code allowedStoreIds}: quando ambos nulos, sem restrição de loja.
 */
@org.springframework.stereotype.Repository
public interface ReportQueryRepository extends Repository<Sale, UUID> {

    @Query(
            """
            SELECT COALESCE(SUM(s.totalAmount), 0), COUNT(s)
            FROM Sale s
            WHERE s.status IN :statuses
              AND s.saleDate >= :from AND s.saleDate < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            """)
    List<Object[]> sumAndCountSales(
            @Param("statuses") Collection<Sale.SaleStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds);

    @Query(
            """
            SELECT s.status, COUNT(s), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.saleDate >= :from AND s.saleDate < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            GROUP BY s.status
            ORDER BY s.status
            """)
    List<Object[]> countSalesByStatus(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds);

    @Query(
            value =
                    """
                    SELECT ((s.sale_date AT TIME ZONE 'UTC')::date) AS day,
                           COALESCE(SUM(s.total_amount), 0),
                           COUNT(*)
                    FROM sales s
                    WHERE s.status IN ('CONFIRMED', 'PAID', 'PARTIALLY_PAID')
                      AND s.sale_date >= :from AND s.sale_date < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR s.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR s.store_id IN (:allowedStoreIds))
                    GROUP BY ((s.sale_date AT TIME ZONE 'UTC')::date)
                    ORDER BY day
                    """,
            nativeQuery = true)
    List<Object[]> salesByDay(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds);

    @Query(
            """
            SELECT p.id, p.sku, p.name, COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.lineTotal), 0)
            FROM SaleItem i
            JOIN i.sale s
            JOIN i.product p
            WHERE s.status IN :statuses
              AND s.saleDate >= :from AND s.saleDate < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            GROUP BY p.id, p.sku, p.name
            ORDER BY SUM(i.lineTotal) DESC
            """)
    List<Object[]> topProducts(
            @Param("statuses") Collection<Sale.SaleStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            """
            SELECT c.id, c.name, c.document, COUNT(DISTINCT s.id), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            JOIN s.customer c
            WHERE s.status IN :statuses
              AND s.saleDate >= :from AND s.saleDate < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            GROUP BY c.id, c.name, c.document
            ORDER BY SUM(s.totalAmount) DESC
            """)
    List<Object[]> topCustomers(
            @Param("statuses") Collection<Sale.SaleStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            """
            SELECT u.id, u.name, COUNT(s), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            JOIN s.seller u
            WHERE s.status IN :statuses
              AND s.saleDate >= :from AND s.saleDate < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            GROUP BY u.id, u.name
            ORDER BY SUM(s.totalAmount) DESC
            """)
    List<Object[]> salesBySeller(
            @Param("statuses") Collection<Sale.SaleStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT s.id, s.sale_number, s.sale_date, s.status, s.total_amount,
                           c.name, u.name
                    FROM sales s
                    LEFT JOIN customers c ON c.id = s.customer_id
                    JOIN users u ON u.id = s.seller_id
                    WHERE (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text))
                      AND s.sale_date >= :from AND s.sale_date < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR s.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR s.store_id IN (:allowedStoreIds))
                    ORDER BY s.sale_date DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM sales s
                    WHERE (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text))
                      AND s.sale_date >= :from AND s.sale_date < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR s.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR s.store_id IN (:allowedStoreIds))
                    """,
            nativeQuery = true)
    Page<Object[]> salesDetail(
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            """
            SELECT COUNT(i)
            FROM Inventory i JOIN i.product p
            WHERE i.quantity < p.minStock
              AND (:storeId IS NULL OR i.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR i.store.id IN :allowedStoreIds)
            """)
    long countStockBelowMinimum(
            @Param("storeId") UUID storeId, @Param("allowedStoreIds") Collection<UUID> allowedStoreIds);

    @Query(
            """
            SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM Payment p
            JOIN p.sale s
            WHERE p.status = :status
              AND p.paidAt IS NOT NULL
              AND p.paidAt >= :from AND p.paidAt < :to
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:allowedStoreIds IS NULL OR s.store.id IN :allowedStoreIds)
            GROUP BY p.method
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> paymentsByMethod(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds);

    @Query(
            value =
                    """
                    SELECT p.id, p.method, p.amount, p.status, p.paid_at, p.external_reference,
                           s.sale_number
                    FROM payments p
                    JOIN sales s ON s.id = p.sale_id
                    WHERE (CAST(:method AS text) IS NULL OR p.method = CAST(:method AS text))
                      AND (CAST(:status AS text) IS NULL OR p.status = CAST(:status AS text))
                      AND p.paid_at IS NOT NULL
                      AND p.paid_at >= :from AND p.paid_at < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR s.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR s.store_id IN (:allowedStoreIds))
                    ORDER BY p.paid_at DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM payments p
                    JOIN sales s ON s.id = p.sale_id
                    WHERE (CAST(:method AS text) IS NULL OR p.method = CAST(:method AS text))
                      AND (CAST(:status AS text) IS NULL OR p.status = CAST(:status AS text))
                      AND p.paid_at IS NOT NULL
                      AND p.paid_at >= :from AND p.paid_at < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR s.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR s.store_id IN (:allowedStoreIds))
                    """,
            nativeQuery = true)
    Page<Object[]> paymentsDetail(
            @Param("method") String method,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT c.id, c.name, c.document, c.type, c.status, c.created_at
                    FROM customers c
                    WHERE c.created_at >= :from AND c.created_at < :to
                    ORDER BY c.created_at DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*) FROM customers c
                    WHERE c.created_at >= :from AND c.created_at < :to
                    """,
            nativeQuery = true)
    Page<Object[]> customersByPeriod(
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query(
            value =
                    """
                    SELECT i.id, p.id, p.sku, p.name, i.quantity, p.min_stock, p.unit_of_measure
                    FROM inventory i
                    JOIN products p ON p.id = i.product_id
                    WHERE (CAST(:storeId AS uuid) IS NULL OR i.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR i.store_id IN (:allowedStoreIds))
                    ORDER BY p.name
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM inventory i
                    WHERE (CAST(:storeId AS uuid) IS NULL OR i.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR i.store_id IN (:allowedStoreIds))
                    """,
            nativeQuery = true)
    Page<Object[]> inventoryCurrent(
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT i.id, p.id, p.sku, p.name, i.quantity, p.min_stock, p.unit_of_measure
                    FROM inventory i
                    JOIN products p ON p.id = i.product_id
                    WHERE i.quantity < p.min_stock
                      AND (CAST(:storeId AS uuid) IS NULL OR i.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR i.store_id IN (:allowedStoreIds))
                    ORDER BY (p.min_stock - i.quantity) DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM inventory i
                    JOIN products p ON p.id = i.product_id
                    WHERE i.quantity < p.min_stock
                      AND (CAST(:storeId AS uuid) IS NULL OR i.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR i.store_id IN (:allowedStoreIds))
                    """,
            nativeQuery = true)
    Page<Object[]> inventoryBelowMinimum(
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT m.id, p.sku, p.name, m.type, m.quantity, m.previous_quantity, m.new_quantity,
                           m.reference_type, m.created_at
                    FROM stock_movements m
                    JOIN products p ON p.id = m.product_id
                    JOIN warehouses w ON w.id = m.warehouse_id
                    WHERE (CAST(:type AS text) IS NULL OR m.type = CAST(:type AS text))
                      AND (CAST(:productId AS uuid) IS NULL OR m.product_id = CAST(:productId AS uuid))
                      AND m.created_at >= :from AND m.created_at < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR w.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR w.store_id IN (:allowedStoreIds))
                    ORDER BY m.created_at DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM stock_movements m
                    JOIN warehouses w ON w.id = m.warehouse_id
                    WHERE (CAST(:type AS text) IS NULL OR m.type = CAST(:type AS text))
                      AND (CAST(:productId AS uuid) IS NULL OR m.product_id = CAST(:productId AS uuid))
                      AND m.created_at >= :from AND m.created_at < :to
                      AND (CAST(:storeId AS uuid) IS NULL OR w.store_id = CAST(:storeId AS uuid))
                      AND (CAST(:restrictAllowedStores AS boolean) = false OR w.store_id IN (:allowedStoreIds))
                    """,
            nativeQuery = true)
    Page<Object[]> stockMovements(
            @Param("type") String type,
            @Param("productId") String productId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("restrictAllowedStores") boolean restrictAllowedStores,
            @Param("allowedStoreIds") Collection<UUID> allowedStoreIds,
            Pageable pageable);
}
