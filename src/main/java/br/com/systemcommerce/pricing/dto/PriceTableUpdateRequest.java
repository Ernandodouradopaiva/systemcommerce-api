package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record PriceTableUpdateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "prioridade é obrigatória") Integer priority,
        @NotNull(message = "status é obrigatório") PriceTable.Status status,
        PriceChannel channel,
        PriceTableScopeType scopeType,
        UUID storeGroupId,
        Instant validFrom,
        Instant validTo) {}
