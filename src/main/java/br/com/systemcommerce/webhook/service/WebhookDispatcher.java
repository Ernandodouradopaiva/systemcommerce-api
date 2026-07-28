package br.com.systemcommerce.webhook.service;

import br.com.systemcommerce.webhook.crypto.WebhookHmacSigner;
import br.com.systemcommerce.webhook.entity.IntegrationOutboxEvent;
import br.com.systemcommerce.webhook.entity.OutboxEventStatus;
import br.com.systemcommerce.webhook.entity.WebhookAttempt;
import br.com.systemcommerce.webhook.entity.WebhookDelivery;
import br.com.systemcommerce.webhook.entity.WebhookDeliveryStatus;
import br.com.systemcommerce.webhook.entity.WebhookSubscription;
import br.com.systemcommerce.webhook.entity.WebhookSubscriptionStatus;
import br.com.systemcommerce.webhook.repository.IntegrationOutboxEventRepository;
import br.com.systemcommerce.webhook.repository.WebhookAttemptRepository;
import br.com.systemcommerce.webhook.repository.WebhookDeliveryRepository;
import br.com.systemcommerce.webhook.repository.WebhookSubscriptionRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final IntegrationOutboxEventRepository outboxRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookAttemptRepository attemptRepository;
    private final WebhookSubscriptionService subscriptionService;
    private final WebhookHmacSigner hmacSigner;
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Scheduled(fixedDelayString = "${systemcommerce.webhooks.outbox-poll-ms:10000}")
    @Transactional
    public void processOutbox() {
        List<IntegrationOutboxEvent> due =
                outboxRepository.findDue(OutboxEventStatus.PENDING, Instant.now());
        for (IntegrationOutboxEvent event : due) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            outboxRepository.save(event);
            List<WebhookSubscription> subs = subscriptionRepository.findByOrganizationIdAndStatusAndActiveTrue(
                    event.getOrganization().getId(), WebhookSubscriptionStatus.ACTIVE);
            for (WebhookSubscription sub : subs) {
                if (!subscribes(sub, event.getEventType())) {
                    continue;
                }
                String deliveryKey = event.getIdempotencyKey() + ":" + sub.getId();
                if (deliveryRepository
                        .findBySubscriptionIdAndIdempotencyKey(sub.getId(), deliveryKey)
                        .isPresent()) {
                    continue;
                }
                WebhookDelivery delivery = new WebhookDelivery();
                delivery.setOrganization(event.getOrganization());
                delivery.setSubscription(sub);
                delivery.setOutboxEvent(event);
                delivery.setEventType(event.getEventType());
                delivery.setPayloadJson(event.getPayloadJson());
                delivery.setPayloadVersion(event.getPayloadVersion());
                delivery.setIdempotencyKey(deliveryKey);
                delivery.setStatus(WebhookDeliveryStatus.PENDING);
                delivery.setNextAttemptAt(Instant.now());
                deliveryRepository.save(delivery);
            }
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            outboxRepository.save(event);
        }
    }

    @Scheduled(fixedDelayString = "${systemcommerce.webhooks.delivery-poll-ms:10000}")
    @Transactional
    public void processDeliveries() {
        List<WebhookDelivery> due = deliveryRepository.findDue(
                List.of(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.FAILED), Instant.now());
        for (WebhookDelivery delivery : due) {
            deliver(delivery);
        }
    }

    @Transactional
    public WebhookDelivery replay(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository
                .findById(deliveryId)
                .orElseThrow(() -> new br.com.systemcommerce.shared.exception.ResourceNotFoundException(
                        "Delivery não encontrada"));
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setNextAttemptAt(Instant.now());
        delivery.setLastError(null);
        deliveryRepository.save(delivery);
        deliver(delivery);
        return delivery;
    }

    private void deliver(WebhookDelivery delivery) {
        WebhookSubscription sub = delivery.getSubscription();
        if (sub.getStatus() != WebhookSubscriptionStatus.ACTIVE) {
            delivery.setStatus(WebhookDeliveryStatus.DEAD_LETTER);
            deliveryRepository.save(delivery);
            return;
        }
        delivery.setStatus(WebhookDeliveryStatus.IN_PROGRESS);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setUpdatedAt(Instant.now());
        deliveryRepository.save(delivery);

        String secret = subscriptionService.decryptSecret(sub);
        String signature = hmacSigner.sign(secret, delivery.getPayloadJson());
        long started = System.currentTimeMillis();
        WebhookAttempt attempt = new WebhookAttempt();
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(delivery.getAttemptCount());
        attempt.setRequestHeadersJson(
                "{\"X-SystemCommerce-Signature\":\"" + signature + "\",\"X-SystemCommerce-Event\":\""
                        + delivery.getEventType() + "\"}");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sub.getTargetUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-SystemCommerce-Signature", signature)
                    .header("X-SystemCommerce-Event", delivery.getEventType())
                    .header("X-SystemCommerce-Delivery-Id", delivery.getId().toString())
                    .header("X-SystemCommerce-Payload-Version", delivery.getPayloadVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayloadJson()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            attempt.setResponseStatus(response.statusCode());
            attempt.setResponseBody(
                    response.body() != null && response.body().length() > 4000
                            ? response.body().substring(0, 4000)
                            : response.body());
            attempt.setDurationMs((int) (System.currentTimeMillis() - started));
            attemptRepository.save(attempt);
            delivery.setLastStatusCode(response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setStatus(WebhookDeliveryStatus.SUCCEEDED);
                delivery.setDeliveredAt(Instant.now());
                sub.setConsecutiveFailures(0);
            } else {
                failDelivery(delivery, sub, "HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            attempt.setErrorMessage(ex.getMessage());
            attempt.setDurationMs((int) (System.currentTimeMillis() - started));
            attemptRepository.save(attempt);
            failDelivery(delivery, sub, ex.getMessage());
        }
        delivery.setUpdatedAt(Instant.now());
        deliveryRepository.save(delivery);
        subscriptionRepository.save(sub);
    }

    private void failDelivery(WebhookDelivery delivery, WebhookSubscription sub, String message) {
        delivery.setLastError(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        sub.setConsecutiveFailures(sub.getConsecutiveFailures() + 1);
        if (sub.getConsecutiveFailures() >= sub.getMaxFailures()) {
            sub.setStatus(WebhookSubscriptionStatus.DISABLED);
            delivery.setStatus(WebhookDeliveryStatus.DEAD_LETTER);
        } else if (delivery.getAttemptCount() >= 8) {
            delivery.setStatus(WebhookDeliveryStatus.DEAD_LETTER);
        } else {
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            long backoff = (long) Math.pow(2, Math.min(delivery.getAttemptCount(), 6)) * 30L;
            delivery.setNextAttemptAt(Instant.now().plusSeconds(backoff));
        }
    }

    private boolean subscribes(WebhookSubscription sub, String eventType) {
        return Arrays.stream(sub.getEventTypes().split("[,\\s]+"))
                .anyMatch(t -> t.equalsIgnoreCase(eventType) || "*".equals(t));
    }
}
