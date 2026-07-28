package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PriceTableCopyRequest(
        UUID sourceTableId,
        UUID sourceStoreId,
        @NotNull(message = "loja destino é obrigatória") UUID targetStoreId,
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String targetCode,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String targetName,
        PriceChannel channel) {}
