package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.constraints.NotBlank;

public record ReceivableWriteOffRequest(@NotBlank String reason) {}
