package br.com.systemcommerce.fiscal.establishment.dto;

public record FiscalEmitterSnapshot(
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
        String taxpayerIndicator) {}
