package br.com.systemcommerce.webhook.repository;

import br.com.systemcommerce.webhook.entity.IntegrationOutboxEvent;
import br.com.systemcommerce.webhook.entity.OutboxEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationOutboxEventRepository extends JpaRepository<IntegrationOutboxEvent, UUID> {

    Optional<IntegrationOutboxEvent> findByOrganizationIdAndIdempotencyKey(
            UUID organizationId, String idempotencyKey);

    @Query("""
            SELECT e FROM IntegrationOutboxEvent e
            WHERE e.status = :status AND e.availableAt <= :now
            ORDER BY e.createdAt ASC
            """)
    List<IntegrationOutboxEvent> findDue(
            @Param("status") OutboxEventStatus status, @Param("now") Instant now);
}
