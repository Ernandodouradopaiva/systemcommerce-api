package br.com.systemcommerce.finance.paymentcatalog.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentMethodStoreConfigRequest(
        @NotNull UUID storeId, Boolean enabled, Boolean allowsPos, Integer maxInstallments, String notes) {}
