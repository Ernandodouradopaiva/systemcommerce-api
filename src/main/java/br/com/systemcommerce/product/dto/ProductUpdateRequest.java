package br.com.systemcommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(
        @NotBlank(message = "código interno é obrigatório") @Size(max = 60) String internalCode,
        @NotBlank(message = "SKU é obrigatório") @Size(max = 60) String sku,
        @Size(max = 60) String barcode,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "categoria é obrigatória") UUID categoryId,
        @NotBlank(message = "unidade de medida é obrigatória") @Size(max = 20) String unitOfMeasure,
        @NotNull(message = "preço de custo é obrigatório")
                @DecimalMin(value = "0.00", inclusive = true, message = "preço de custo não pode ser negativo")
                BigDecimal costPrice,
        @NotNull(message = "preço de venda é obrigatório")
                @DecimalMin(value = "0.00", inclusive = true, message = "preço de venda não pode ser negativo")
                BigDecimal salePrice,
        @NotNull(message = "estoque mínimo é obrigatório")
                @DecimalMin(value = "0.000", inclusive = true, message = "estoque mínimo não pode ser negativo")
                BigDecimal minStock,
        Boolean allowNegativeStock,
        @Size(max = 500) String imageUrl,
        UUID brandId,
        UUID manufacturerId,
        UUID productLineId) {}
