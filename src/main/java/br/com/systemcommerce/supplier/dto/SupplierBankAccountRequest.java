package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import jakarta.validation.constraints.Size;

public record SupplierBankAccountRequest(
        @Size(max = 10) String bankCode,
        @Size(max = 20) String agency,
        @Size(max = 30) String account,
        SupplierBankAccount.BankAccountType accountType,
        @Size(max = 140) String pixKey,
        @Size(max = 150) String holderName,
        Boolean active) {}
