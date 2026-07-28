package br.com.systemcommerce.batch.dto;

import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ProductBatchCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID productId,
        @NotBlank String batchCode,
        UUID supplierId,
        LocalDate manufacturedAt,
        LocalDate expiresAt,
        String notes) {}
