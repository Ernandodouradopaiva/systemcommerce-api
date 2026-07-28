package br.com.systemcommerce.fiscal.integration.returns.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PurchaseReturnEmitRequest(@NotNull UUID supplierReturnId) {}
