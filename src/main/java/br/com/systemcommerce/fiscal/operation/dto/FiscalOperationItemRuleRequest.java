package br.com.systemcommerce.fiscal.operation.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FiscalOperationItemRuleRequest(
        @Size(max = 10) String ncmPrefix,
        UUID productId,
        @Size(max = 10) String cfopOverride,
        @Size(max = 40) String taxRuleCode) {}
