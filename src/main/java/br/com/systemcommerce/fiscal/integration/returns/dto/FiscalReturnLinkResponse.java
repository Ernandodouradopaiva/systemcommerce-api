package br.com.systemcommerce.fiscal.integration.returns.dto;

import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.LinkStatus;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.ReturnType;
import java.util.UUID;

public record FiscalReturnLinkResponse(
        UUID id,
        ReturnType returnType,
        UUID returnId,
        UUID fiscalDocumentId,
        UUID originalDocumentId,
        LinkStatus status) {}
