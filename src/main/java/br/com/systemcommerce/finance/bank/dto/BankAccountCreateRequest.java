package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.BankAccount;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankAccountCreateRequest(
        @NotNull UUID organizationId,
        UUID storeId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID bankId,
        @NotBlank String agency,
        @NotBlank String accountNumber,
        String accountDigit,
        @NotNull BankAccount.AccountKind accountKind,
        @NotBlank String holderName,
        String holderDocument,
        String currency,
        @NotNull @DecimalMin("0.00") BigDecimal openingBalance,
        LocalDate openingBalanceDate,
        Boolean allowsPayments,
        Boolean allowsReceipts,
        Boolean allowsReconciliation) {}
