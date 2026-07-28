package br.com.systemcommerce.carrier.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FreightRegionResponse(
        UUID id,
        String regionCode,
        String regionName,
        String zipFrom,
        String zipTo,
        BigDecimal minWeight,
        BigDecimal maxWeight,
        BigDecimal minVolume,
        BigDecimal maxVolume,
        BigDecimal minOrderAmount,
        BigDecimal freightAmount,
        Integer leadTimeDays) {}
