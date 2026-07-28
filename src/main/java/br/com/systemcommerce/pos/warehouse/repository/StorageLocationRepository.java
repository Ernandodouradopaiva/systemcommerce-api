package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {

    @EntityGraph(attributePaths = {"warehouse", "zone", "aisle", "rack", "shelf"})
    List<StorageLocation> findByWarehouse_IdOrderByCodeAsc(UUID warehouseId);

    boolean existsByWarehouse_IdAndCodeIgnoreCase(UUID warehouseId, String code);

    boolean existsByZone_Id(UUID zoneId);

    boolean existsByAisle_Id(UUID aisleId);

    boolean existsByRack_Id(UUID rackId);

    boolean existsByShelf_Id(UUID shelfId);

    @EntityGraph(attributePaths = {"warehouse", "zone", "aisle", "rack", "shelf"})
    @Query("SELECT sl FROM StorageLocation sl WHERE sl.id = :id")
    Optional<StorageLocation> findDetailedById(@Param("id") UUID id);
}
