package br.com.systemcommerce.storeproduct.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductWithoutConfigResponse(
        UUID id, String sku, String name, String internalCode, BigDecimal salePrice) {}
