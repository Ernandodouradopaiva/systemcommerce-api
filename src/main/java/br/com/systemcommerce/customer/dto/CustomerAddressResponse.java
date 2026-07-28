package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerAddress;
import java.time.Instant;
import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        UUID customerId,
        CustomerAddress.AddressType type,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        Boolean isDefault,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
