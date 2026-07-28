package br.com.systemcommerce.webhook.outbox;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.webhook.entity.IntegrationOutboxEvent;
import br.com.systemcommerce.webhook.entity.OutboxEventStatus;
import br.com.systemcommerce.webhook.repository.IntegrationOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica eventos na mesma TX do domínio — HTTP só no worker (Prompt 82).
 */
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final IntegrationOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(
            Organization organization,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            Map<String, Object> payload,
            String idempotencyKey) {
        var existing = outboxRepository.findByOrganizationIdAndIdempotencyKey(
                organization.getId(), idempotencyKey);
        if (existing.isPresent()) {
            return;
        }
        try {
            IntegrationOutboxEvent event = new IntegrationOutboxEvent();
            event.setOrganization(organization);
            event.setEventType(eventType);
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setPayloadJson(objectMapper.writeValueAsString(payload));
            event.setPayloadVersion("v1");
            event.setIdempotencyKey(idempotencyKey);
            event.setStatus(OutboxEventStatus.PENDING);
            event.setAvailableAt(Instant.now());
            outboxRepository.save(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gravar outbox", ex);
        }
    }
}
