package br.com.systemcommerce.fiscal.taxation.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.systemcommerce.fiscal.taxation.engine.ConditionOperator;

public record TaxRuleConditionRequest(
        @NotBlank @Size(max = 60) String fieldName,
        @NotNull ConditionOperator operator,
        @Size(max = 500) String valueText,
        Integer sortOrder) {}
