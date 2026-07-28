package br.com.systemcommerce.uom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnitOfMeasureUpdateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 80) String name,
        @Size(max = 500) String description,
        @Size(max = 20) String symbol,
        @Min(0) @Max(8) Integer precisionScale) {}
