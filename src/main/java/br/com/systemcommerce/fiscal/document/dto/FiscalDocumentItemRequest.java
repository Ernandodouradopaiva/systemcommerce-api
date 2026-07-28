package br.com.systemcommerce.fiscal.document.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record FiscalDocumentItemRequest(
        UUID productId,
        String productSnapshotJson,
        @Size(max = 10) String ncm,
        @Size(max = 10) String cest,
        @Size(max = 10) String cfop,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        String taxSnapshotJson,
        @Size(max = 10) String commercialUom,
        @Size(max = 10) String taxableUom) {}
