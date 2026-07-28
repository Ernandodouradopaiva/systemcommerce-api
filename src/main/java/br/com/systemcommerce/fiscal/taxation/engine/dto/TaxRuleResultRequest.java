package br.com.systemcommerce.fiscal.taxation.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TaxRuleResultRequest(
        @NotBlank @Size(max = 60) String resultKey,
        @Size(max = 200) String resultValue,
        BigDecimal numericValue) {}
