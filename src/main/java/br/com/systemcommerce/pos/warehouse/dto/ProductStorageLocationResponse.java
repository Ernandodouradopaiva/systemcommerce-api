package br.com.systemcommerce.pos.warehouse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductStorageLocationResponse(
        UUID id,
        UUID productId,
        UUID storageLocationId,
        String storageLocationCode,
        Boolean preferred,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        BigDecimal quantityAtLocation,
        Boolean active) {}
