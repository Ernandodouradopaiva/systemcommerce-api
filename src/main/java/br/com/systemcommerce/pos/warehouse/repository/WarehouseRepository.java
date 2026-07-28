package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID>, JpaSpecificationExecutor<Warehouse> {

    boolean existsByStoreIdAndCodeIgnoreCase(UUID storeId, String code);

    boolean existsByStoreIdAndCodeIgnoreCaseAndIdNot(UUID storeId, String code, UUID id);

    boolean existsByStoreId(UUID storeId);

    long countByStoreId(UUID storeId);

    @EntityGraph(attributePaths = {"store"})
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
    Optional<Warehouse> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT w FROM Warehouse w JOIN FETCH w.store s
            WHERE s.code = 'LOJA-01' AND w.code = 'DEP-01'
            """)
    Optional<Warehouse> findDefaultSeedWarehouse();

    /** Primeiro depósito utilizável (ativo, não bloqueado, permite venda) da loja — uso best-effort (ex.: reserva de orçamento). */
    @Query(
            """
            SELECT w FROM Warehouse w
            WHERE w.store.id = :storeId AND w.status = 'ACTIVE' AND w.allowsSale = true
                AND w.blockedForMovement = false
            ORDER BY w.code ASC
            """)
    List<Warehouse> findUsableSaleWarehousesByStoreId(@Param("storeId") UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Warehouse w JOIN FETCH w.store WHERE w.id = :id")
    Optional<Warehouse> findByIdForUpdate(@Param("id") UUID id);
}
