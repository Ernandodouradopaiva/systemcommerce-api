package br.com.systemcommerce.hierarchy.repository;

import br.com.systemcommerce.hierarchy.entity.UserHierarchyAssignment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserHierarchyAssignmentRepository extends JpaRepository<UserHierarchyAssignment, UUID> {

    @Query(
            """
            select uha from UserHierarchyAssignment uha
            where uha.manager.id = :managerId
              and uha.active = true
              and uha.status = br.com.systemcommerce.hierarchy.entity.UserHierarchyAssignment.Status.ACTIVE
              and uha.validFrom <= :now
              and (uha.validTo is null or uha.validTo >= :now)
            """)
    List<UserHierarchyAssignment> findDirectReports(
            @Param("managerId") UUID managerId, @Param("now") Instant now);

    @Query(
            """
            select uha from UserHierarchyAssignment uha
            where uha.user.id = :userId
              and uha.active = true
              and uha.status = br.com.systemcommerce.hierarchy.entity.UserHierarchyAssignment.Status.ACTIVE
              and uha.validFrom <= :now
              and (uha.validTo is null or uha.validTo >= :now)
            """)
    List<UserHierarchyAssignment> findEffectiveByUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
