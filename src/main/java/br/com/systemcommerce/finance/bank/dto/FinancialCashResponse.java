package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialCash;
import java.math.BigDecimal;
import java.util.UUID;

public record FinancialCashResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String code,
        String name,
        FinancialCash.CashKind cashKind,
        UUID posTerminalId,
        UUID linkedCashSessionId,
        BigDecimal openingBalance,
        BigDecimal currentBalance,
        FinancialAccountHolder.HolderStatus status,
        boolean usable,
        Long version) {}
