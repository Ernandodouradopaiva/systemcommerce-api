package br.com.systemcommerce.finance.payable.dto;

import br.com.systemcommerce.finance.payable.entity.PayableOrigin;
import java.util.UUID;

public record PayableOriginResponse(
        UUID id, PayableOrigin.OriginType originType, UUID originDocumentId, String originDocumentNumber) {}
