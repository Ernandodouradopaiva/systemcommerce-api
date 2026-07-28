package br.com.systemcommerce.carrier.dto;

import br.com.systemcommerce.carrier.entity.FreightMode;
import java.util.UUID;

public record FreightModeResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        FreightMode.ModeType modeType,
        FreightMode.FreightModeStatus status,
        boolean usable) {}
