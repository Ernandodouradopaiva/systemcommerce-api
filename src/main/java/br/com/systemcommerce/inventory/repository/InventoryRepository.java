package br.com.systemcommerce.inventory.repository;

import br.com.systemcommerce.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {

    @Query("SELECT COUNT(i) > 0 FROM Inventory i WHERE i.product.id = :productId")
    boolean existsByProductId(@Param("productId") UUID productId);

    @Query(
            """
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            JOIN FETCH i.store s
            WHERE i.product.id = :productId
            """)
    List<Inventory> findAllByProductId(@Param("productId") UUID productId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.product.id = :productId")
    BigDecimal sumQuantityByProductId(@Param("productId") UUID productId);

    @Query(
            """
            SELECT i.product.id, COALESCE(SUM(i.quantity), 0)
            FROM Inventory i
            WHERE i.product.id IN :productIds
            GROUP BY i.product.id
            """)
    List<Object[]> findQuantityRowsByProductIds(@Param("productIds") Collection<UUID> productIds);

    @Query(
            """
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            LEFT JOIN FETCH i.store s
            WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId
            """)
    Optional<Inventory> findByProductIdAndWarehouseId(
            @Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            LEFT JOIN FETCH i.store s
            WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId
            """)
    Optional<Inventory> findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);

    @Query(
            value =
                    """
                    SELECT i FROM Inventory i JOIN FETCH i.product p JOIN FETCH i.warehouse w
                    WHERE i.quantity < p.minStock
                    """,
            countQuery =
                    """
                    SELECT COUNT(i) FROM Inventory i JOIN i.product p
                    WHERE i.quantity < p.minStock
                    """)
    Page<Inventory> findBelowMinimum(Pageable pageable);
}
