package br.com.systemcommerce.storeaccess.repository;

import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStoreAccessRepository extends JpaRepository<UserStoreAccess, UUID> {

    @EntityGraph(attributePaths = {"store", "store.organization", "user", "grantedBy"})
    @Query("SELECT a FROM UserStoreAccess a WHERE a.id = :id")
    Optional<UserStoreAccess> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"store", "store.organization"})
    @Query(
            """
            SELECT a FROM UserStoreAccess a
            WHERE a.user.id = :userId
            ORDER BY a.startDate DESC, a.createdAt DESC
            """)
    List<UserStoreAccess> findHistoryByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"store", "store.organization"})
    @Query(
            """
            SELECT a FROM UserStoreAccess a
            WHERE a.user.id = :userId
              AND a.status = 'ACTIVE'
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
            ORDER BY a.defaultStore DESC, a.store.code ASC
            """)
    List<UserStoreAccess> findEffectiveByUserId(
            @Param("userId") UUID userId, @Param("onDate") LocalDate onDate);

    @Query(
            """
            SELECT COUNT(a) > 0 FROM UserStoreAccess a
            WHERE a.user.id = :userId
              AND a.store.id = :storeId
              AND a.status = 'ACTIVE'
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
            """)
    boolean hasEffectiveAccess(
            @Param("userId") UUID userId, @Param("storeId") UUID storeId, @Param("onDate") LocalDate onDate);

    Optional<UserStoreAccess> findFirstByUserIdAndDefaultStoreTrueAndStatus(
            UUID userId, UserStoreAccess.AccessStatus status);
}
