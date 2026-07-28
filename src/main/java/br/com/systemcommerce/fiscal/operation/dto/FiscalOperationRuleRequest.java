package br.com.systemcommerce.fiscal.operation.dto;

import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import jakarta.validation.constraints.Size;

public record FiscalOperationRuleRequest(
        @Size(max = 2) String originUf,
        @Size(max = 2) String destUf,
        TaxpayerIndicator taxpayerIndicator,
        Boolean finalConsumer,
        @Size(max = 10) String cfop,
        @Size(max = 40) String taxRuleCode,
        Integer priority) {}
