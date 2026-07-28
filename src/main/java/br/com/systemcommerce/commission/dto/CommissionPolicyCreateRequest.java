package br.com.systemcommerce.commission.dto;

import br.com.systemcommerce.commission.entity.CommissionPolicy.PolicyChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionPolicyCreateRequest(
        UUID organizationId,
        @NotBlank String code,
        @NotBlank String name,
        UUID storeId,
        UUID sellerProfileId,
        UUID productId,
        UUID categoryId,
        PolicyChannel channel,
        @NotNull BigDecimal percent,
        BigDecimal fixedAmount,
        Boolean requiresPaid,
        Boolean appliesOnConfirmed,
        Instant validFrom,
        Instant validTo) {}
