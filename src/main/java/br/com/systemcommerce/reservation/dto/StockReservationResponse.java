package br.com.systemcommerce.reservation.dto;

import br.com.systemcommerce.reservation.entity.StockReservation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReservationResponse(
        UUID id,
        String reservationNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        StockReservation.OriginType originType,
        UUID originId,
        String originNumber,
        StockReservation.ReservationStatus status,
        Instant expiresAt,
        String notes,
        List<StockReservationItemResponse> items,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
