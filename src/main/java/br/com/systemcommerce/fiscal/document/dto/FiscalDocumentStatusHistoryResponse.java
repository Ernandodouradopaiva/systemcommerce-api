package br.com.systemcommerce.fiscal.document.dto;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record FiscalDocumentStatusHistoryResponse(
        UUID id,
        FiscalDocumentStatus fromStatus,
        FiscalDocumentStatus toStatus,
        Instant at,
        UUID byUser,
        String sefazCstat,
        String sefazXmotivo,
        String details) {}
