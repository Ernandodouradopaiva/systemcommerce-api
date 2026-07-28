package br.com.systemcommerce.fiscal.operation.dto;

import java.util.UUID;

public record FiscalOperationItemRuleResponse(
        UUID id, String ncmPrefix, UUID productId, String cfopOverride, String taxRuleCode) {}
