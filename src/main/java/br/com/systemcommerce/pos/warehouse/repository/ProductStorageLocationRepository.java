package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.ProductStorageLocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStorageLocationRepository extends JpaRepository<ProductStorageLocation, UUID> {

    @EntityGraph(attributePaths = "storageLocation")
    List<ProductStorageLocation> findByProduct_Id(UUID productId);

    boolean existsByProduct_IdAndStorageLocation_Id(UUID productId, UUID storageLocationId);

    boolean existsByStorageLocation_Id(UUID storageLocationId);
}
