package br.com.systemcommerce.fiscal.document.dto;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import jakarta.validation.constraints.NotNull;

public record FiscalDocumentStatusTransitionRequest(
        @NotNull FiscalDocumentStatus toStatus,
        String sefazCstat,
        String sefazXmotivo,
        String details) {}
