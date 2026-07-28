package br.com.systemcommerce.fiscal.monitoring.dto;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FiscalMonitorDocumentResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID establishmentId,
        String model,
        String series,
        Long number,
        String accessKey,
        FiscalDocumentStatus status,
        String sefazCstat,
        String sefazXmotivo,
        BigDecimal totalInvoice,
        Boolean contingency,
        String environment,
        Instant createdAt,
        boolean communicationFailureLikely) {}
