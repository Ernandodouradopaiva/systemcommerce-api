package br.com.systemcommerce.fiscal.validation.dto;

public record FiscalXmlValidationMessage(String code, String message, String field, boolean warning) {}
