package br.com.systemcommerce.finance.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BankCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String shortName,
        @Size(max = 2) String countryCode) {}
