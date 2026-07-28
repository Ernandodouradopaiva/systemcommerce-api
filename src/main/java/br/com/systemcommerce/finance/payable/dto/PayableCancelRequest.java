package br.com.systemcommerce.finance.payable.dto;

import jakarta.validation.constraints.NotBlank;

public record PayableCancelRequest(@NotBlank String reason) {}
