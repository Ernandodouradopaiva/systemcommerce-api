package br.com.systemcommerce.integration.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChannelOrderItemResponse(
        UUID id,
        UUID productId,
        String externalSku,
        String title,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {}
