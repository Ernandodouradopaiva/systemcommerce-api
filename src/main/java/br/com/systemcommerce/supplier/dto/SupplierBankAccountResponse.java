package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import java.time.Instant;
import java.util.UUID;

public record SupplierBankAccountResponse(
        UUID id,
        UUID supplierId,
        String bankCode,
        String agency,
        String account,
        SupplierBankAccount.BankAccountType accountType,
        String pixKey,
        String holderName,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
