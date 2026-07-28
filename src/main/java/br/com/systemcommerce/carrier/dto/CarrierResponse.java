package br.com.systemcommerce.carrier.dto;

import br.com.systemcommerce.carrier.entity.Carrier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CarrierResponse(
        UUID id,
        UUID organizationId,
        String code,
        String legalName,
        String tradeName,
        String document,
        String stateRegistration,
        String anttRntrc,
        Carrier.CarrierStatus status,
        boolean usable,
        String notes,
        List<CarrierContactResponse> contacts,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
