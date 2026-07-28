package br.com.systemcommerce.fiscal.contingency.dto;

import br.com.systemcommerce.fiscal.contingency.entity.ContingencyDocument.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record ContingencyDocumentResponse(
        UUID id,
        UUID contingencyId,
        UUID documentId,
        Boolean pendingRetransmission,
        Instant lastConsultAt,
        DocumentStatus status) {}
