package br.com.systemcommerce.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillOfMaterialsItemResponse(
        UUID id,
        UUID componentProductId,
        String componentSku,
        BigDecimal quantity,
        String unitCode,
        BigDecimal scrapPercent,
        Integer lineNumber) {}
