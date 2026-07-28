package br.com.systemcommerce.fiscal.taxation.engine.dto;

public record TaxRuleConditionResponse(
        java.util.UUID id, String fieldName, String operator, String valueText, Integer sortOrder) {}
