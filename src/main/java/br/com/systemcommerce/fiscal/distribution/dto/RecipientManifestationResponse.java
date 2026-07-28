package br.com.systemcommerce.fiscal.distribution.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipientManifestationResponse(
        UUID id,
        String accessKey,
        String currentType,
        String status,
        boolean conclusive,
        String protocol,
        Instant authorizedAt,
        List<EventItem> events) {

    public record EventItem(
            int sequence,
            String eventType,
            String status,
            String protocol,
            String cstat,
            String xmotivo,
            Instant transmittedAt) {}
}
