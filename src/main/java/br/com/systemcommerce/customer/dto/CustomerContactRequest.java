package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerContact;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerContactRequest(
        @NotNull(message = "tipo de contato é obrigatório") CustomerContact.ContactType type,
        @Size(max = 150) String name,
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 30) String mobile,
        @Size(max = 100) String roleDescription,
        Boolean isDefault,
        @Size(max = 500) String notes) {}
