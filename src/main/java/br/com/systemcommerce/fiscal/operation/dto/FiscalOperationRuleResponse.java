package br.com.systemcommerce.fiscal.operation.dto;

import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import java.util.UUID;

public record FiscalOperationRuleResponse(
        UUID id,
        String originUf,
        String destUf,
        TaxpayerIndicator taxpayerIndicator,
        Boolean finalConsumer,
        String cfop,
        String taxRuleCode,
        Integer priority) {}
