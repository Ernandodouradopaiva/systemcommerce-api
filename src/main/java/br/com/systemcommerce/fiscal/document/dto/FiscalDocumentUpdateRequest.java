package br.com.systemcommerce.fiscal.document.dto;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

public record FiscalDocumentUpdateRequest(
        String natureOfOperation,
        String purpose,
        UUID carrierId,
        @Valid List<FiscalDocumentItemRequest> items,
        @Valid List<FiscalDocumentPaymentRequest> payments) {}
