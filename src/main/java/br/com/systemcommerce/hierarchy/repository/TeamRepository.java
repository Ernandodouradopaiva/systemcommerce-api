package br.com.systemcommerce.hierarchy.repository;

import br.com.systemcommerce.hierarchy.entity.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOrganizationIdAndActiveTrueOrderByNameAsc(UUID organizationId);

    Optional<Team> findByOrganizationIdAndCode(UUID organizationId, String code);
}
