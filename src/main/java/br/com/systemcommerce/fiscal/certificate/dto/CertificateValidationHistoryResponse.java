package br.com.systemcommerce.fiscal.certificate.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateValidationHistoryResponse(
        UUID id, UUID certificateId, Instant validatedAt, UUID validatedBy, String result, String message) {}
