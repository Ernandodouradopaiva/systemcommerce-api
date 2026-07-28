package br.com.systemcommerce.hierarchy.repository;

import br.com.systemcommerce.hierarchy.entity.OrganizationalPosition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationalPositionRepository extends JpaRepository<OrganizationalPosition, UUID> {
    List<OrganizationalPosition> findByOrganizationIdAndActiveTrueOrderByLevelRankAsc(UUID organizationId);

    Optional<OrganizationalPosition> findByOrganizationIdAndCode(UUID organizationId, String code);
}
