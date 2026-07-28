package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierContact;
import java.time.Instant;
import java.util.UUID;

public record SupplierContactResponse(
        UUID id,
        UUID supplierId,
        SupplierContact.ContactType type,
        String name,
        String phone,
        String email,
        String role,
        Boolean primary,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
