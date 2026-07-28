package br.com.systemcommerce.fiscal.numbering.dto;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberVoidingRequest.VoidingStatus;
import java.time.Instant;
import java.util.UUID;

public record FiscalNumberVoidingResponse(
        UUID id,
        UUID establishmentId,
        String model,
        String series,
        Long fromNumber,
        Long toNumber,
        VoidingStatus status,
        String protocolNumber,
        String sefazCstat,
        String sefazXmotivo,
        Instant transmittedAt,
        String idempotencyKey) {}
