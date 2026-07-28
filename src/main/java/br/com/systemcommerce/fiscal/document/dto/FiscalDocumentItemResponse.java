package br.com.systemcommerce.fiscal.document.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalDocumentItemResponse(
        UUID id,
        Integer lineNumber,
        UUID productId,
        String ncm,
        String cest,
        String cfop,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String taxSnapshotJson,
        String commercialUom,
        String taxableUom) {}
