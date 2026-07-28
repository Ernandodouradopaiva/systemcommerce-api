package br.com.systemcommerce.reservation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReservationItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        Integer lineNumber,
        BigDecimal quantityReserved,
        BigDecimal quantityConsumed,
        BigDecimal quantityReleased,
        BigDecimal quantityRemaining) {}
