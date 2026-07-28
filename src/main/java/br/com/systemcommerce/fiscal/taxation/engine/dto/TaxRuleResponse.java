package br.com.systemcommerce.fiscal.taxation.engine.dto;

import br.com.systemcommerce.fiscal.taxation.engine.TaxKind;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaxRuleResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        TaxKind taxKind,
        Integer priority,
        TaxRule.RuleStatus status,
        LocalDate validFrom,
        LocalDate validUntil,
        String versionCode,
        List<TaxRuleConditionResponse> conditions,
        List<TaxRuleResultResponse> results,
        Instant createdAt) {}
