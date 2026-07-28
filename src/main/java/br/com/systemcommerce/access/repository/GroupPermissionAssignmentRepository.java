package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupPermissionAssignmentRepository extends JpaRepository<GroupPermissionAssignment, UUID> {
    @Query(
            """
            select gpa from GroupPermissionAssignment gpa
            join fetch gpa.permission
            where gpa.group.id = :groupId
            """)
    List<GroupPermissionAssignment> findAllByGroupId(@Param("groupId") UUID groupId);

    Optional<GroupPermissionAssignment> findByGroupIdAndPermissionId(UUID groupId, UUID permissionId);

    @Query(
            """
            select gpa from GroupPermissionAssignment gpa
            join fetch gpa.permission
            where gpa.group.id = :groupId and gpa.active = true
            """)
    List<GroupPermissionAssignment> findByGroupIdAndActiveTrue(@Param("groupId") UUID groupId);

    @Query(
            """
            select gpa from GroupPermissionAssignment gpa
            join fetch gpa.permission p
            join fetch gpa.group g
            where g.id in :groupIds
              and gpa.active = true
              and gpa.status = br.com.systemcommerce.access.entity.GroupPermissionAssignment.Status.ACTIVE
              and gpa.grantType = br.com.systemcommerce.access.entity.GroupPermissionAssignment.GrantType.ALLOW
              and (gpa.validTo is null or gpa.validTo >= :now)
              and gpa.validFrom <= :now
              and p.active = true
              and g.active = true
            """)
    List<GroupPermissionAssignment> findEffectiveAssignments(
            @Param("groupIds") List<UUID> groupIds, @Param("now") java.time.Instant now);

    @Query(
            """
            select distinct p.code from GroupPermissionAssignment gpa
            join gpa.permission p
            join gpa.group g
            where g.id in :groupIds
              and gpa.active = true
              and gpa.status = br.com.systemcommerce.access.entity.GroupPermissionAssignment.Status.ACTIVE
              and gpa.grantType = br.com.systemcommerce.access.entity.GroupPermissionAssignment.GrantType.ALLOW
              and (gpa.validTo is null or gpa.validTo >= :now)
              and gpa.validFrom <= :now
              and p.active = true
              and g.active = true
            """)
    List<String> findEffectivePermissionCodes(@Param("groupIds") List<UUID> groupIds, @Param("now") java.time.Instant now);
}
