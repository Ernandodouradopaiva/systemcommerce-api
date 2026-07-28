package br.com.systemcommerce.fiscal.event.cce.dto;

import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetter.Status;
import java.time.Instant;
import java.util.UUID;

public record CorrectionLetterResponse(
        UUID id,
        UUID documentId,
        Integer sequence,
        String correctionText,
        Status status,
        String protocolNumber,
        String sefazCstat,
        String sefazXmotivo,
        Instant transmittedAt,
        String idempotencyKey,
        String validationWarnings) {}
