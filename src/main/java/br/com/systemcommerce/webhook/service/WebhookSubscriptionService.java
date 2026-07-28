package br.com.systemcommerce.webhook.service;

import br.com.systemcommerce.integration.crypto.SecretEncryptionService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.webhook.dto.WebhookSubscriptionCreateRequest;
import br.com.systemcommerce.webhook.dto.WebhookSubscriptionResponse;
import br.com.systemcommerce.webhook.entity.WebhookSecret;
import br.com.systemcommerce.webhook.entity.WebhookSubscription;
import br.com.systemcommerce.webhook.entity.WebhookSubscriptionStatus;
import br.com.systemcommerce.webhook.repository.WebhookSecretRepository;
import br.com.systemcommerce.webhook.repository.WebhookSubscriptionRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookSecretRepository secretRepository;
    private final OrganizationService organizationService;
    private final SecretEncryptionService secretEncryptionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<WebhookSubscriptionResponse> list(UUID organizationId, Pageable pageable) {
        return subscriptionRepository
                .findAll(
                        (root, q, cb) ->
                                organizationId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(s -> toResponse(s, null));
    }

    @Transactional
    public WebhookSubscriptionResponse create(WebhookSubscriptionCreateRequest request) {
        var org = organizationService.resolveForStoreCreate(request.organizationId());
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String plainSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        WebhookSubscription sub = new WebhookSubscription();
        sub.setOrganization(org);
        sub.setName(request.name().trim());
        sub.setTargetUrl(request.targetUrl().trim());
        sub.setEventTypes(request.eventTypes().trim());
        sub.setSecretEncrypted(secretEncryptionService.encrypt(plainSecret));
        sub.setPayloadVersion("v1");
        sub.setStatus(WebhookSubscriptionStatus.ACTIVE);
        sub.setConsecutiveFailures(0);
        sub.setMaxFailures(request.maxFailures() != null && request.maxFailures() > 0 ? request.maxFailures() : 10);
        WebhookSubscription saved = subscriptionRepository.save(sub);

        WebhookSecret secret = new WebhookSecret();
        secret.setSubscription(saved);
        secret.setSecretEncrypted(saved.getSecretEncrypted());
        secretRepository.save(secret);
        return toResponse(saved, plainSecret);
    }

    @Transactional
    public WebhookSubscriptionResponse disable(UUID id) {
        WebhookSubscription sub = subscriptionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription não encontrada"));
        sub.setStatus(WebhookSubscriptionStatus.DISABLED);
        sub.setActive(Boolean.FALSE);
        return toResponse(subscriptionRepository.save(sub), null);
    }

    public String decryptSecret(WebhookSubscription sub) {
        return secretEncryptionService.decrypt(sub.getSecretEncrypted());
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription s, String plainSecret) {
        return new WebhookSubscriptionResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getName(),
                s.getTargetUrl(),
                s.getEventTypes(),
                s.getPayloadVersion(),
                s.getStatus(),
                s.getConsecutiveFailures(),
                plainSecret);
    }
}
