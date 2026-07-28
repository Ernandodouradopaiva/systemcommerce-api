package br.com.systemcommerce.pos.sale.dto;

import java.time.Instant;
import java.util.UUID;

public record SuspendedSaleExpirationResponse(
        UUID saleId,
        String saleNumber,
        Instant suspendedAt,
        Instant suspendExpiresAt,
        boolean expired,
        Long remainingSeconds,
        String message) {}
