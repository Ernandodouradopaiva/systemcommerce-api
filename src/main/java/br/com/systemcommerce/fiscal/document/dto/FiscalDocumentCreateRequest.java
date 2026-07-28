package br.com.systemcommerce.fiscal.document.dto;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.party.PartyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record FiscalDocumentCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID establishmentId,
        @NotNull UUID storeId,
        @NotBlank @Size(max = 10) String model,
        @NotBlank @Size(max = 10) String series,
        @NotNull FiscalEstablishment.FiscalEnvironment environment,
        String natureOfOperation,
        String purpose,
        UUID operationId,
        FiscalDocument.DocumentDirection direction,
        PartyType recipientPartyType,
        UUID recipientPartyId,
        UUID carrierId,
        UUID taxCalculationId,
        String originDocumentType,
        UUID originDocumentId,
        @NotBlank @Size(max = 100) String idempotencyKey,
        Boolean contingency,
        @NotEmpty @Valid List<FiscalDocumentItemRequest> items,
        @Valid List<FiscalDocumentPaymentRequest> payments) {}
