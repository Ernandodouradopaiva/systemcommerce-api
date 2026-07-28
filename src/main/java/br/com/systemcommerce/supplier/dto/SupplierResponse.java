package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.Supplier;
import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String code,
        Supplier.PersonType type,
        String document,
        String stateRegistration,
        String legalName,
        String tradeName,
        String contactName,
        String phone,
        String mobile,
        String email,
        String website,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String notes,
        String municipalRegistration,
        Supplier.TaxContributorIndicator taxContributorIndicator,
        String category,
        Supplier.SupplierStatus status,
        Instant blockedAt,
        String blockedReason,
        Boolean active,
        Instant registeredAt,
        Instant createdAt,
        Instant updatedAt) {}
