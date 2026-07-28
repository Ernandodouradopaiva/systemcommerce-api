package br.com.systemcommerce.pos.warehouse.repository;

import br.com.systemcommerce.pos.warehouse.entity.WarehouseShelf;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseShelfRepository extends JpaRepository<WarehouseShelf, UUID> {

    List<WarehouseShelf> findByRack_IdOrderByCodeAsc(UUID rackId);

    boolean existsByRack_IdAndCodeIgnoreCase(UUID rackId, String code);
}
