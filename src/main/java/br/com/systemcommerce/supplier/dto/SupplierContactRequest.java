package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierContactRequest(
        @NotNull(message = "tipo de contato é obrigatório") SupplierContact.ContactType type,
        @NotBlank(message = "nome é obrigatório") @Size(max = 150) String name,
        @Size(max = 30) String phone,
        @Size(max = 255) String email,
        @Size(max = 100) String role,
        Boolean primary) {}
