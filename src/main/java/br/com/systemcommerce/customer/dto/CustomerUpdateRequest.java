package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CustomerUpdateRequest(
        @NotNull(message = "tipo de pessoa é obrigatório") Customer.CustomerType type,
        @NotBlank(message = "nome/razão social é obrigatório") @Size(max = 200) String name,
        @Size(max = 200) String tradeName,
        @NotBlank(message = "CPF/CNPJ é obrigatório") @Size(max = 20) String document,
        @Size(max = 30) String stateRegistration,
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 30) String mobile,
        LocalDate birthDate,
        @Size(max = 2000) String notes,
        @Size(max = 10) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Size(max = 2) String state,
        Customer.CustomerClassification classification,
        Customer.RegistrationOrigin registrationOrigin,
        @Size(max = 2000) String commercialNotes,
        @Size(max = 30) String municipalRegistration,
        Boolean allowQuoteWhenBlocked) {}
