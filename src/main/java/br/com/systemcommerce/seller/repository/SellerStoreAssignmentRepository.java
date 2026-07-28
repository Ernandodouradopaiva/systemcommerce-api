package br.com.systemcommerce.seller.repository;

import br.com.systemcommerce.seller.entity.SellerStoreAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerStoreAssignmentRepository extends JpaRepository<SellerStoreAssignment, UUID> {

    @EntityGraph(attributePaths = {"store", "sellerProfile", "sellerProfile.employee"})
    @Query("SELECT a FROM SellerStoreAssignment a WHERE a.id = :id")
    Optional<SellerStoreAssignment> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"store"})
    @Query(
            """
            SELECT a FROM SellerStoreAssignment a
            WHERE a.sellerProfile.id = :sellerId
            ORDER BY a.startDate DESC, a.createdAt DESC
            """)
    List<SellerStoreAssignment> findHistoryBySellerId(@Param("sellerId") UUID sellerId);

    @EntityGraph(attributePaths = {"store", "sellerProfile", "sellerProfile.employee"})
    @Query(
            """
            SELECT a FROM SellerStoreAssignment a
            WHERE a.store.id = :storeId
              AND a.status = 'ACTIVE'
              AND a.allowsRegisterSale = TRUE
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
              AND a.sellerProfile.status = 'ACTIVE'
            ORDER BY a.primaryAssignment DESC, a.sellerProfile.sellerCode ASC
            """)
    List<SellerStoreAssignment> findActiveSellersByStore(
            @Param("storeId") UUID storeId, @Param("onDate") LocalDate onDate);

    @Query(
            """
            SELECT a FROM SellerStoreAssignment a
            WHERE a.sellerProfile.id = :sellerId
              AND a.store.id = :storeId
              AND a.status = 'ACTIVE'
              AND a.allowsRegisterSale = TRUE
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
            """)
    Optional<SellerStoreAssignment> findEffectiveAuthorization(
            @Param("sellerId") UUID sellerId, @Param("storeId") UUID storeId, @Param("onDate") LocalDate onDate);
}
