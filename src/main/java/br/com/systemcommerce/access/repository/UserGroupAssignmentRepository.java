package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.UserGroupAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGroupAssignmentRepository extends JpaRepository<UserGroupAssignment, UUID> {
    List<UserGroupAssignment> findByUserIdAndActiveTrue(UUID userId);

    List<UserGroupAssignment> findByGroupIdAndActiveTrue(UUID groupId);

    Optional<UserGroupAssignment> findByUserIdAndGroupIdAndStoreId(UUID userId, UUID groupId, UUID storeId);

    @Query(
            """
            select uga from UserGroupAssignment uga
            where uga.user.id = :userId and uga.group.id = :groupId
              and ((:storeId is null and uga.store is null) or uga.store.id = :storeId)
            """)
    Optional<UserGroupAssignment> findByUserAndGroupAndStore(
            @Param("userId") UUID userId, @Param("groupId") UUID groupId, @Param("storeId") UUID storeId);

    long countByGroupIdAndStatusAndActiveTrue(UUID groupId, UserGroupAssignment.Status status);

    @Query(
            """
            select uga from UserGroupAssignment uga
            join fetch uga.group g
            where uga.user.id = :userId
              and uga.active = true
              and uga.status = br.com.systemcommerce.access.entity.UserGroupAssignment.Status.ACTIVE
              and uga.validFrom <= :now
              and (uga.validTo is null or uga.validTo >= :now)
              and g.active = true
            """)
    List<UserGroupAssignment> findEffectiveAssignments(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query(
            """
            select count(distinct uga.user.id) from UserGroupAssignment uga
            join uga.group g
            where uga.active = true
              and uga.status = br.com.systemcommerce.access.entity.UserGroupAssignment.Status.ACTIVE
              and uga.validFrom <= :now
              and (uga.validTo is null or uga.validTo >= :now)
              and g.active = true
              and (g.code = :adminCode or g.allowsAdministration = true)
              and uga.id <> :excludingId
            """)
    long countOtherAdminUsers(
            @Param("now") Instant now,
            @Param("adminCode") String adminCode,
            @Param("excludingId") UUID excludingId);
}
