package br.com.systemcommerce.commission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CommissionAdjustmentRequest(
        @NotNull UUID calculationId, @NotNull BigDecimal amount, @NotBlank String reason) {}
