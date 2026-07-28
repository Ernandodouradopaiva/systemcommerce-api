package br.com.systemcommerce.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationCreateRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "razão social é obrigatória") @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @Size(max = 20) String document,
        @Size(max = 30) String stateRegistration,
        @Size(max = 30) String municipalRegistration,
        @Email(message = "e-mail inválido") @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 255) String website,
        @Size(max = 10) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Size(max = 2) String state,
        @Size(max = 64) String defaultTimezone,
        @Size(min = 3, max = 3) String currency) {}
