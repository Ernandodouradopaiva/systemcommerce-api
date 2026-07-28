package br.com.systemcommerce.customer.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerCommercialConditionResponse(
        UUID id,
        UUID customerId,
        Integer paymentTermDays,
        String paymentCondition,
        UUID priceTableId,
        String priceTableName,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
