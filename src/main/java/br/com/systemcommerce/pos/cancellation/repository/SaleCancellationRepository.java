package br.com.systemcommerce.pos.cancellation.repository;

import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleCancellationRepository
        extends JpaRepository<SaleCancellation, UUID>, JpaSpecificationExecutor<SaleCancellation> {

    Optional<SaleCancellation> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"sale", "requestedBy", "authorizedBy", "executedBy", "refunds", "refunds.payment"})
    @Query("SELECT c FROM SaleCancellation c WHERE c.id = :id")
    Optional<SaleCancellation> findDetailedById(@Param("id") UUID id);

    List<SaleCancellation> findBySaleIdOrderByRequestedAtDesc(UUID saleId);

    boolean existsBySaleIdAndStatusIn(UUID saleId, List<SaleCancellation.Status> statuses);
}
