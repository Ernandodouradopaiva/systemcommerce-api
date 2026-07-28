package br.com.systemcommerce.fiscal.numbering.dto;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record FiscalNumberReservationResponse(
        UUID id,
        UUID sequenceId,
        Long number,
        Instant reservedAt,
        Instant expiresAt,
        UUID documentId,
        ReservationStatus status,
        String idempotencyKey) {}
