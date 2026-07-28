package br.com.systemcommerce.pos.cash.dto;

import jakarta.validation.constraints.Size;

public record CashMovementReverseRequest(@Size(max = 1000) String description) {}
