package br.com.systemcommerce.fiscal.distribution.dto;

import java.util.UUID;

public record DfeDistributionDocumentResponse(
        UUID id,
        UUID establishmentId,
        Long nsu,
        String schemaType,
        String accessKey,
        String status,
        boolean recognized,
        boolean suspicious,
        String suspiciousReason,
        UUID incomingDocumentId,
        String xmlSha256) {}
