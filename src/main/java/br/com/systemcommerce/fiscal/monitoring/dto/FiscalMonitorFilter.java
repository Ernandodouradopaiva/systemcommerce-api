package br.com.systemcommerce.fiscal.monitoring.dto;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.time.Instant;
import java.util.UUID;

public record FiscalMonitorFilter(
        UUID organizationId,
        UUID storeId,
        UUID establishmentId,
        String model,
        String series,
        Long number,
        String accessKey,
        Instant periodFrom,
        Instant periodTo,
        FiscalDocumentStatus status,
        String sefazCstat,
        String environment,
        UUID createdBy) {}
