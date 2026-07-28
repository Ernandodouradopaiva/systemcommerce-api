package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.StoreGroupMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreGroupMemberRepository extends JpaRepository<StoreGroupMember, UUID> {

    Optional<StoreGroupMember> findByStoreGroupIdAndStoreId(UUID storeGroupId, UUID storeId);

    @Query(
            """
            SELECT m.store.id FROM StoreGroupMember m
            WHERE m.storeGroup.id = :groupId
              AND m.active = TRUE
              AND m.storeGroup.active = TRUE
            """)
    List<UUID> findStoreIdsByStoreGroupId(@Param("groupId") UUID groupId);

    @Query(
            """
            SELECT m.storeGroup.id FROM StoreGroupMember m
            WHERE m.store.id = :storeId
              AND m.active = TRUE
              AND m.storeGroup.active = TRUE
              AND m.storeGroup.status = 'ACTIVE'
            """)
    List<UUID> findActiveGroupIdsByStoreId(@Param("storeId") UUID storeId);
}
