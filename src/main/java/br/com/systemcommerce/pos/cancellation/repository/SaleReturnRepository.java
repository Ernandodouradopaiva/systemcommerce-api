package br.com.systemcommerce.pos.cancellation.repository;

import br.com.systemcommerce.pos.cancellation.entity.SaleReturn;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID> {

    Optional<SaleReturn> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"originalSale", "cashSession", "requestedBy", "items", "items.product"})
    @Query("SELECT r FROM SaleReturn r WHERE r.id = :id")
    Optional<SaleReturn> findDetailedById(@Param("id") UUID id);

    long countByReturnNumberStartingWith(String prefix);
}
