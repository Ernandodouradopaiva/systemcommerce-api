package br.com.systemcommerce.reservation.dto;

import br.com.systemcommerce.reservation.entity.StockReservation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReservationCreateRequest(
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @NotNull StockReservation.OriginType originType,
        @NotNull UUID originId,
        @Size(max = 40) String originNumber,
        Instant expiresAt,
        @Size(max = 2000) String notes,
        String idempotencyKey,
        @NotEmpty @Valid List<StockReservationItemRequest> items) {}
