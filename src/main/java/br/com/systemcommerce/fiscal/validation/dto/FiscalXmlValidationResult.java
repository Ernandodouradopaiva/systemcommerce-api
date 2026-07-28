package br.com.systemcommerce.fiscal.validation.dto;

import java.util.List;

public record FiscalXmlValidationResult(
        boolean valid, List<FiscalXmlValidationMessage> messages, boolean schemaSoftPass) {}
