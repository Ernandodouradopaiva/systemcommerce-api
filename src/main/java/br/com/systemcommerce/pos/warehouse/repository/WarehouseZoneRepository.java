package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.WarehouseZone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseZoneRepository extends JpaRepository<WarehouseZone, UUID> {

    List<WarehouseZone> findByWarehouse_IdOrderByCodeAsc(UUID warehouseId);

    boolean existsByWarehouse_IdAndCodeIgnoreCase(UUID warehouseId, String code);
}
