package br.com.systemcommerce.integration.dto;

import br.com.systemcommerce.integration.entity.ChannelOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChannelOrderResponse(
        UUID id,
        UUID marketplaceAccountId,
        UUID salesOrderId,
        String externalOrderId,
        String externalStatus,
        ChannelOrderStatus status,
        String buyerName,
        BigDecimal totalAmount,
        String currency,
        Instant receivedAt,
        Instant convertedAt,
        List<ChannelOrderItemResponse> items) {}
