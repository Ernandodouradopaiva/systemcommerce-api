package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerConsent;
import java.time.Instant;
import java.util.UUID;

public record CustomerConsentResponse(
        UUID id,
        UUID customerId,
        CustomerConsent.ConsentType type,
        Boolean granted,
        Instant grantedAt,
        Instant revokedAt,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
