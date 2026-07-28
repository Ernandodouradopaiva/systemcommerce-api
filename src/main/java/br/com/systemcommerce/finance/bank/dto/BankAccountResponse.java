package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.BankAccount;
import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String code,
        String name,
        UUID bankId,
        String bankCode,
        String bankName,
        String agency,
        String accountNumber,
        String accountDigit,
        BankAccount.AccountKind accountKind,
        String holderName,
        String holderDocument,
        String currency,
        BigDecimal openingBalance,
        LocalDate openingBalanceDate,
        BigDecimal currentBalance,
        boolean allowsPayments,
        boolean allowsReceipts,
        boolean allowsReconciliation,
        FinancialAccountHolder.HolderStatus status,
        boolean usable,
        Long version) {}
