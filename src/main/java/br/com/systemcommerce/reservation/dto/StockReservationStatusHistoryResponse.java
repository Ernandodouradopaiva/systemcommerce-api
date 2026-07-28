package br.com.systemcommerce.reservation.dto;

import br.com.systemcommerce.reservation.entity.StockReservation;
import java.time.Instant;
import java.util.UUID;

public record StockReservationStatusHistoryResponse(
        UUID id,
        StockReservation.ReservationStatus fromStatus,
        StockReservation.ReservationStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
