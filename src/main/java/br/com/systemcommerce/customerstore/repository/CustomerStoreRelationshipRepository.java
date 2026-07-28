package br.com.systemcommerce.customerstore.repository;

import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationship;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationshipStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerStoreRelationshipRepository extends JpaRepository<CustomerStoreRelationship, UUID> {

    @EntityGraph(attributePaths = {"customer", "store", "preferredSellerProfile"})
    Optional<CustomerStoreRelationship> findByCustomerIdAndStoreId(UUID customerId, UUID storeId);

    @EntityGraph(attributePaths = {"customer", "store", "preferredSellerProfile"})
    @Query(
            """
            SELECT r FROM CustomerStoreRelationship r
            WHERE r.store.id = :storeId
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<CustomerStoreRelationship> findByStoreId(
            @Param("storeId") UUID storeId,
            @Param("status") CustomerStoreRelationshipStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "store", "preferredSellerProfile"})
    @Query(
            """
            SELECT r FROM CustomerStoreRelationship r
            WHERE r.customer.id = :customerId
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<CustomerStoreRelationship> findByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("status") CustomerStoreRelationshipStatus status,
            Pageable pageable);

    boolean existsByCustomerIdAndStoreId(UUID customerId, UUID storeId);
}
