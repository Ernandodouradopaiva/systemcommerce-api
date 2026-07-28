package br.com.systemcommerce.finance.receivable.dto;

import br.com.systemcommerce.finance.receivable.entity.ReceivableOrigin;
import java.util.UUID;

public record ReceivableOriginResponse(
        UUID id,
        ReceivableOrigin.OriginType originType,
        UUID originDocumentId,
        String originDocumentNumber) {}
