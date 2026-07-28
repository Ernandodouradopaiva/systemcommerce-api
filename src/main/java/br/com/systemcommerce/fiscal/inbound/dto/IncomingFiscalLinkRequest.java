package br.com.systemcommerce.fiscal.inbound.dto;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocumentLink.LinkType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IncomingFiscalLinkRequest(@NotNull LinkType linkType, @NotNull UUID linkId) {}
