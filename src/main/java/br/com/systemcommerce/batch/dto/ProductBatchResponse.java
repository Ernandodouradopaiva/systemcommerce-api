package br.com.systemcommerce.batch.dto;

import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductBatchResponse(
        UUID id,
        UUID organizationId,
        UUID productId,
        String productSku,
        String productName,
        String batchCode,
        LocalDate manufacturedAt,
        LocalDate expiresAt,
        Instant receivedAt,
        ProductBatchStatus status,
        String notes,
        Boolean active) {}
