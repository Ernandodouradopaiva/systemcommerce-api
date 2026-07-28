package br.com.systemcommerce.shipment.dto;

import java.math.BigDecimal;

public record ShipmentPackageRequest(
        BigDecimal weight, BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm, String trackingCode) {}
