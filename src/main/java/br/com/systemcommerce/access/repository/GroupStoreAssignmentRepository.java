package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.GroupStoreAssignment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupStoreAssignmentRepository extends JpaRepository<GroupStoreAssignment, UUID> {
    List<GroupStoreAssignment> findByGroupIdAndActiveTrue(UUID groupId);

    @Query(
            """
            select gsa.store.id from GroupStoreAssignment gsa
            where gsa.group.id in :groupIds
              and gsa.active = true
              and gsa.status = br.com.systemcommerce.access.entity.GroupStoreAssignment.Status.ACTIVE
              and gsa.validFrom <= :now
              and (gsa.validTo is null or gsa.validTo >= :now)
            """)
    List<UUID> findEffectiveStoreIds(@Param("groupIds") List<UUID> groupIds, @Param("now") Instant now);
}
