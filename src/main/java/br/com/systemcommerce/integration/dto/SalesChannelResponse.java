package br.com.systemcommerce.integration.dto;

import br.com.systemcommerce.integration.entity.SalesChannelType;
import java.util.UUID;

public record SalesChannelResponse(
        UUID id, UUID organizationId, String code, String name, SalesChannelType channelType, Boolean active) {}
