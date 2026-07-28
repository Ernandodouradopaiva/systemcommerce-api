package br.com.systemcommerce.carrier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CarrierCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @NotBlank @Size(max = 20) String document,
        @Size(max = 30) String stateRegistration,
        @Size(max = 40) String anttRntrc,
        @Size(max = 2000) String notes,
        @Valid List<CarrierContactRequest> contacts) {}
