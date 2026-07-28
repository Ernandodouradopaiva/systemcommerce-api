package br.com.systemcommerce.fiscal.taxation.dto;

import jakarta.validation.constraints.Size;

public record ProductTaxClassificationRequest(
        @Size(max = 40) String taxType,
        @Size(max = 10) String cstOrCsosn,
        @Size(max = 10) String cfopCode,
        String extraJson) {}
