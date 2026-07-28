package br.com.systemcommerce.customerstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerStoreRelationshipNotesRequest(
        @Size(max = 2000) String localNotes,
        @DecimalMin(value = "0", message = "limite de crédito não pode ser negativo") BigDecimal creditLimitOverride) {}
