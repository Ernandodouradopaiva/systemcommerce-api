package br.com.systemcommerce.webhook.dto;

import br.com.systemcommerce.webhook.entity.WebhookSubscriptionStatus;
import java.util.UUID;

public record WebhookSubscriptionResponse(
        UUID id,
        UUID organizationId,
        String name,
        String targetUrl,
        String eventTypes,
        String payloadVersion,
        WebhookSubscriptionStatus status,
        Integer consecutiveFailures,
        /** Secret plaintext só na criação. */
        String signingSecret) {}
