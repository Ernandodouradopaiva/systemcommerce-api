package br.com.systemcommerce.fiscal.certificate.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateUsageLogResponse(
        UUID id,
        UUID certificateId,
        UUID establishmentId,
        Instant usedAt,
        String purpose,
        String correlationId,
        UUID performedBy) {}
