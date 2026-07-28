package br.com.systemcommerce.fiscal.operation.dto;

import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FiscalOperationCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String natureOfOperation,
        CalculationPurpose purpose,
        @Size(max = 20) String allowedModels,
        @Size(max = 10) String defaultCfop,
        Boolean generatesFinance,
        Boolean movesStock,
        FiscalOperation.StockDirection stockDirection,
        Boolean requiresReferencedDocument,
        Boolean allowsFinalConsumer,
        @NotNull LocalDate validFrom,
        LocalDate validUntil,
        @Valid List<FiscalOperationRuleRequest> rules,
        @Valid List<FiscalOperationItemRuleRequest> itemRules,
        List<UUID> storeIds) {}
