package br.com.systemcommerce.webhook.repository;

import br.com.systemcommerce.webhook.entity.WebhookDelivery;
import br.com.systemcommerce.webhook.entity.WebhookDeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Optional<WebhookDelivery> findBySubscriptionIdAndIdempotencyKey(UUID subscriptionId, String idempotencyKey);

    @Query("""
            SELECT d FROM WebhookDelivery d
            WHERE d.status IN :statuses
              AND (d.nextAttemptAt IS NULL OR d.nextAttemptAt <= :now)
            ORDER BY d.createdAt ASC
            """)
    List<WebhookDelivery> findDue(
            @Param("statuses") List<WebhookDeliveryStatus> statuses, @Param("now") Instant now);
}
