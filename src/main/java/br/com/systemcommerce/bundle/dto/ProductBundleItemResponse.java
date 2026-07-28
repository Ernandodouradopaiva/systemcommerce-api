package br.com.systemcommerce.bundle.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductBundleItemResponse(
        UUID id,
        UUID componentProductId,
        String componentSku,
        String componentName,
        BigDecimal quantity,
        Integer lineNumber,
        Boolean optionalComponent) {}
