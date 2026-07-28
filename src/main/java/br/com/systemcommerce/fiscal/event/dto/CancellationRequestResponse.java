package br.com.systemcommerce.fiscal.event.dto;

import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationRequest.CancellationStatus;
import java.time.Instant;
import java.util.UUID;

public record CancellationRequestResponse(
        UUID id,
        UUID documentId,
        CancellationStatus status,
        String justification,
        String protocolNumber,
        String sefazCstat,
        Instant transmittedAt,
        String idempotencyKey) {}
