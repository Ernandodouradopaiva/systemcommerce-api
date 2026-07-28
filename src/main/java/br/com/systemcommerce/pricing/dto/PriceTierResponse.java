package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceTierResponse(
        UUID id, UUID productPriceId, BigDecimal minQuantity, BigDecimal maxQuantity, BigDecimal unitPrice,
        Boolean active) {}
