package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerContact;
import java.time.Instant;
import java.util.UUID;

public record CustomerContactResponse(
        UUID id,
        UUID customerId,
        CustomerContact.ContactType type,
        String name,
        String email,
        String phone,
        String mobile,
        String roleDescription,
        Boolean isDefault,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
