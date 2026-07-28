package br.com.systemcommerce.hierarchy.repository;

import br.com.systemcommerce.hierarchy.entity.TeamManagerAssignment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamManagerAssignmentRepository extends JpaRepository<TeamManagerAssignment, UUID> {

    @Query(
            """
            select tma.team.id from TeamManagerAssignment tma
            where tma.manager.id = :managerId
              and tma.active = true
              and tma.status = br.com.systemcommerce.hierarchy.entity.TeamManagerAssignment.Status.ACTIVE
              and tma.validFrom <= :now
              and (tma.validTo is null or tma.validTo >= :now)
              and tma.team.active = true
            """)
    List<UUID> findManagedTeamIds(@Param("managerId") UUID managerId, @Param("now") Instant now);
}
