package br.com.systemcommerce.finance.billing.repository;

import br.com.systemcommerce.finance.billing.entity.BillingWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, UUID> {
    Optional<BillingWebhookEvent> findByOrganizationIdAndProviderCodeAndEventId(
            UUID organizationId, String providerCode, String eventId);
}
