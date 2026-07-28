package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerConsent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerConsentRequest(
        @NotNull(message = "tipo de consentimento é obrigatório") CustomerConsent.ConsentType type,
        @NotNull(message = "informe se o consentimento foi concedido") Boolean granted,
        @Size(max = 500) String notes) {}
