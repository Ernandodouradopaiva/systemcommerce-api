package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.FreightRegion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreightRegionRepository extends JpaRepository<FreightRegion, UUID> {

    List<FreightRegion> findByFreightTableIdAndActiveTrue(UUID freightTableId);
}
