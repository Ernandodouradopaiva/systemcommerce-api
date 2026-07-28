package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.WarehouseRack;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRackRepository extends JpaRepository<WarehouseRack, UUID> {

    List<WarehouseRack> findByAisle_IdOrderByCodeAsc(UUID aisleId);

    boolean existsByAisle_IdAndCodeIgnoreCase(UUID aisleId, String code);
}
