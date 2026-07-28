package br.com.systemcommerce.quote.dto;

import jakarta.validation.constraints.Size;

public record QuoteAcceptanceRequest(
        @Size(max = 200) String acceptedByName,
        @Size(max = 255) String acceptedByEmail,
        @Size(max = 80) String acceptanceToken,
        @Size(max = 40) String channel,
        @Size(max = 1000) String notes) {}
