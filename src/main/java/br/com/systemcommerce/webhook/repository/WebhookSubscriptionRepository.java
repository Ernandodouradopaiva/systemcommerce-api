package br.com.systemcommerce.webhook.repository;

import br.com.systemcommerce.webhook.entity.WebhookSubscription;
import br.com.systemcommerce.webhook.entity.WebhookSubscriptionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, UUID>, JpaSpecificationExecutor<WebhookSubscription> {

    List<WebhookSubscription> findByOrganizationIdAndStatusAndActiveTrue(
            UUID organizationId, WebhookSubscriptionStatus status);
}
