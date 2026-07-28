package br.com.systemcommerce.fiscal.establishment.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalEstablishmentResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        String legalName,
        String tradeName,
        String cnpj,
        String stateRegistration,
        String municipalRegistration,
        String cnaePrincipal,
        String ibgeCityCode,
        String uf,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String phone,
        String email,
        String taxRegime,
        Short crt,
        String taxpayerIndicator,
        FiscalEstablishment.FiscalEnvironment fiscalEnvironment,
        String defaultNfeSeries,
        String defaultNfceSeries,
        boolean allowsNfe,
        boolean allowsNfce,
        FiscalEstablishment.EstablishmentStatus status,
        boolean usable,
        LocalDate accreditationDate,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
