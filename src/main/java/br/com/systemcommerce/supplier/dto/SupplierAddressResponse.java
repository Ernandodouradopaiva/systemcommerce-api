package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierAddress;
import java.time.Instant;
import java.util.UUID;

public record SupplierAddressResponse(
        UUID id,
        UUID supplierId,
        SupplierAddress.AddressType type,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        Boolean primary,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
