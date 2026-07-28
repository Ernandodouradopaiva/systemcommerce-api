package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.WarehouseAisle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseAisleRepository extends JpaRepository<WarehouseAisle, UUID> {

    List<WarehouseAisle> findByZone_IdOrderByCodeAsc(UUID zoneId);

    boolean existsByZone_IdAndCodeIgnoreCase(UUID zoneId, String code);
}
