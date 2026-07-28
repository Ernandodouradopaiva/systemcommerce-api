package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.constraints.NotBlank;

public record ReceivableCancelRequest(@NotBlank String reason) {}
