package br.com.systemcommerce.fiscal.inbound.dto;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument.Status;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomingFiscalDocumentResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID supplierId,
        String accessKey,
        String model,
        String series,
        Long number,
        LocalDate issueDate,
        Status status,
        String authorizationProtocol,
        Boolean signatureValid,
        Boolean authorized,
        Instant importedAt) {}
