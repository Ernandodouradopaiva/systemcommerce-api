package br.com.systemcommerce.pos.cancellation.repository;

import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CancellationRefundRepository extends JpaRepository<CancellationRefund, UUID> {

    List<CancellationRefund> findByCancellationIdOrderByCreatedAtAsc(UUID cancellationId);

    @EntityGraph(attributePaths = {"payment", "cancellation"})
    @Query("SELECT r FROM CancellationRefund r WHERE r.id = :id")
    Optional<CancellationRefund> findDetailedById(@Param("id") UUID id);

    Optional<CancellationRefund> findByIdempotencyKey(String idempotencyKey);
}
