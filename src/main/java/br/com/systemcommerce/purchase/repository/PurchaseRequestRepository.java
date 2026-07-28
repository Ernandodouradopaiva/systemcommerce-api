package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    @Query(
            """
            SELECT DISTINCT r FROM PurchaseRequest r
            LEFT JOIN FETCH r.items
            LEFT JOIN FETCH r.requester
            LEFT JOIN FETCH r.buyer
            LEFT JOIN FETCH r.store
            LEFT JOIN FETCH r.organization
            LEFT JOIN FETCH r.warehouse
            WHERE r.id = :id
            """)
    Optional<PurchaseRequest> findDetailedById(@Param("id") UUID id);
}
