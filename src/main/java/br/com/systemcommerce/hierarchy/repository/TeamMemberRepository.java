package br.com.systemcommerce.hierarchy.repository;

import br.com.systemcommerce.hierarchy.entity.TeamMember;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    List<TeamMember> findByTeamIdAndActiveTrue(UUID teamId);

    @Query(
            """
            select tm.user.id from TeamMember tm
            where tm.team.id in :teamIds
              and tm.active = true
              and tm.status = br.com.systemcommerce.hierarchy.entity.TeamMember.Status.ACTIVE
              and tm.validFrom <= :now
              and (tm.validTo is null or tm.validTo >= :now)
            """)
    List<UUID> findActiveMemberUserIds(@Param("teamIds") List<UUID> teamIds, @Param("now") Instant now);
}
