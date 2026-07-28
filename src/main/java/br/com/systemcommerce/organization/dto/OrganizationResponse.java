package br.com.systemcommerce.organization.dto;

import br.com.systemcommerce.organization.entity.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String code,
        String legalName,
        String tradeName,
        String document,
        String stateRegistration,
        String municipalRegistration,
        String email,
        String phone,
        String website,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String defaultTimezone,
        String currency,
        Organization.OrganizationStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
