package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAccountResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String providerCode,
        String providerName,
        String externalAccountId,
        BigDecimal currentBalance,
        FinancialAccountHolder.HolderStatus status,
        boolean usable,
        Long version) {}
