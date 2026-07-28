package br.com.systemcommerce.fiscal.taxation.engine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TaxCalculationItemRequest(
        UUID productId,
        @Size(max = 10) String ncm,
        @Size(max = 10) String cest,
        @Size(max = 5) String originCode,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        @Size(max = 10) String commercialUom) {}
