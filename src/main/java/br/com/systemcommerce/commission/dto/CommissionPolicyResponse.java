package br.com.systemcommerce.commission.dto;

import br.com.systemcommerce.commission.entity.CommissionPolicy.PolicyChannel;
import br.com.systemcommerce.commission.entity.CommissionPolicy.PolicyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionPolicyResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        Integer policyVersion,
        UUID storeId,
        UUID sellerProfileId,
        UUID productId,
        UUID categoryId,
        PolicyChannel channel,
        BigDecimal percent,
        BigDecimal fixedAmount,
        boolean requiresPaid,
        boolean appliesOnConfirmed,
        Instant validFrom,
        Instant validTo,
        PolicyStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
