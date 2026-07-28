package br.com.systemcommerce.shipment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentPackageResponse(
        UUID id,
        int packageNumber,
        BigDecimal weight,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        String trackingCode) {}
