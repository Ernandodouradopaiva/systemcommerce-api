package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.Acquirer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcquirerRepository extends JpaRepository<Acquirer, UUID> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    List<Acquirer> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Acquirer> findById(UUID id);
}