package br.com.systemcommerce.fiscal.distribution.repository;

import br.com.systemcommerce.fiscal.distribution.entity.DfeSequenceControl;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DfeSequenceControlRepository extends JpaRepository<DfeSequenceControl, UUID> {
    Optional<DfeSequenceControl> findByEstablishmentId(UUID establishmentId);
}
