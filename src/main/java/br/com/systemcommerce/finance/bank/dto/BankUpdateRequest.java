package br.com.systemcommerce.finance.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankUpdateRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String shortName,
        @Size(max = 2) String countryCode) {}
