package br.com.systemcommerce.fiscal.operation.dto;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FiscalOperationResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String natureOfOperation,
        CalculationPurpose purpose,
        String allowedModels,
        String defaultCfop,
        Boolean generatesFinance,
        Boolean movesStock,
        FiscalOperation.StockDirection stockDirection,
        Boolean requiresReferencedDocument,
        Boolean allowsFinalConsumer,
        LocalDate validFrom,
        LocalDate validUntil,
        FiscalOperation.OperationStatus status,
        List<FiscalOperationRuleResponse> rules,
        List<FiscalOperationItemRuleResponse> itemRules,
        List<UUID> storeIds,
        Instant createdAt) {}
