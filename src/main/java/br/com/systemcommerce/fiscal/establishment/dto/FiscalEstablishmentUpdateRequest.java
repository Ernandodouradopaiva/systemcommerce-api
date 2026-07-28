package br.com.systemcommerce.fiscal.establishment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FiscalEstablishmentUpdateRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @NotBlank @Size(min = 14, max = 14) String cnpj,
        @Size(max = 30) String stateRegistration,
        @Size(max = 30) String municipalRegistration,
        @Size(max = 10) String cnaePrincipal,
        @NotBlank @Size(min = 7, max = 7) String ibgeCityCode,
        @NotBlank @Size(min = 2, max = 2) String uf,
        @Size(max = 8) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 120) String city,
        @Size(max = 30) String phone,
        @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 40) String taxRegime,
        @NotNull Short crt,
        @NotBlank @Size(max = 40) String taxpayerIndicator,
        @Size(max = 10) String defaultNfeSeries,
        @Size(max = 10) String defaultNfceSeries,
        Boolean allowsNfe,
        Boolean allowsNfce,
        LocalDate accreditationDate) {}
