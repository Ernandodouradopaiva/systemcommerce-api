package br.com.systemcommerce.pos.store.dto;

import br.com.systemcommerce.pos.store.entity.Store;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID organizationId,
        String organizationCode,
        String code,
        String name,
        String tradeName,
        String document,
        String stateRegistration,
        String municipalRegistration,
        Store.EstablishmentType establishmentType,
        boolean headquarters,
        LocalDate openingDate,
        boolean allowsSales,
        boolean allowsPos,
        String email,
        String phone,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String timezone,
        Store.StoreStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
