package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.Supplier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierCreateRequest(
        @NotBlank(message = "código interno é obrigatório") @Size(max = 30) String code,
        @NotNull(message = "tipo de pessoa é obrigatório") Supplier.PersonType type,
        @NotBlank(message = "CPF/CNPJ é obrigatório") @Size(max = 20) String document,
        @Size(max = 30) String stateRegistration,
        @NotBlank(message = "razão social/nome é obrigatório") @Size(max = 200) String legalName,
        @Size(max = 200) String tradeName,
        @Size(max = 150) String contactName,
        @Size(max = 30) String phone,
        @Size(max = 30) String mobile,
        @Size(max = 255) String email,
        @Size(max = 255) String website,
        @Size(max = 10) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Size(max = 2) String state,
        @Size(max = 2000) String notes,
        @Size(max = 30) String municipalRegistration,
        Supplier.TaxContributorIndicator taxContributorIndicator,
        @Size(max = 60) String category) {}
