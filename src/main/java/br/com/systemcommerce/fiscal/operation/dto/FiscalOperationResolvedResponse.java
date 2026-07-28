package br.com.systemcommerce.fiscal.operation.dto;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import java.util.UUID;

public record FiscalOperationResolvedResponse(
        UUID operationId,
        String operationCode,
        String cfop,
        String natureOfOperation,
        CalculationPurpose purpose,
        String taxRuleCode,
        Boolean generatesFinance,
        Boolean movesStock,
        FiscalOperation.StockDirection stockDirection,
        Boolean requiresReferencedDocument) {}
