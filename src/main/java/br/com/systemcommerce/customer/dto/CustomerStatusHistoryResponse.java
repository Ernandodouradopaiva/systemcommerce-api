package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.Customer;
import java.time.Instant;
import java.util.UUID;

public record CustomerStatusHistoryResponse(
        UUID id,
        UUID customerId,
        Customer.CustomerStatus previousStatus,
        Customer.CustomerStatus newStatus,
        String reason,
        UUID changedByUserId,
        String changedByUserName,
        Instant changedAt) {}
