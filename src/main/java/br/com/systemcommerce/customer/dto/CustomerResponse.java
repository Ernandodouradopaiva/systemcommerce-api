package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.Customer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        Customer.CustomerType type,
        String name,
        String tradeName,
        String document,
        String stateRegistration,
        String email,
        String phone,
        String mobile,
        LocalDate birthDate,
        String notes,
        Customer.CustomerStatus status,
        Boolean active,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        Customer.CustomerClassification classification,
        Customer.RegistrationOrigin registrationOrigin,
        String commercialNotes,
        BigDecimal creditLimit,
        Boolean delinquencyIndicator,
        Instant blockedAt,
        String blockedReason,
        Boolean allowQuoteWhenBlocked,
        String municipalRegistration,
        Boolean usableForSale,
        Boolean usableForQuote,
        Instant createdAt,
        Instant updatedAt) {}
