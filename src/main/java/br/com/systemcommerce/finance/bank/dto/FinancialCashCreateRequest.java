package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.FinancialCash;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialCashCreateRequest(
        @NotNull UUID organizationId,
        UUID storeId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @NotNull FinancialCash.CashKind cashKind,
        UUID posTerminalId,
        @NotNull @DecimalMin("0.00") BigDecimal openingBalance,
        LocalDate openingBalanceDate) {}
