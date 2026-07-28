package br.com.systemcommerce.pos.terminal.repository;

import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosTerminalRepository
        extends JpaRepository<PosTerminal, UUID>, JpaSpecificationExecutor<PosTerminal> {

    boolean existsByStoreIdAndCodeIgnoreCase(UUID storeId, String code);

    boolean existsByStoreIdAndCodeIgnoreCaseAndIdNot(UUID storeId, String code, UUID id);

    boolean existsByStoreIdAndTerminalNumber(UUID storeId, Integer terminalNumber);

    boolean existsByStoreIdAndTerminalNumberAndIdNot(UUID storeId, Integer terminalNumber, UUID id);

    boolean existsByStoreId(UUID storeId);

    long countByStoreId(UUID storeId);

    boolean existsByWarehouseId(UUID warehouseId);

    @EntityGraph(attributePaths = {"store", "warehouse", "warehouse.store"})
    @Query("SELECT t FROM PosTerminal t WHERE t.id = :id")
    Optional<PosTerminal> findDetailedById(@Param("id") UUID id);
}
