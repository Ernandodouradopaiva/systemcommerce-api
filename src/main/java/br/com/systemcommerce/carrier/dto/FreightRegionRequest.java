package br.com.systemcommerce.carrier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FreightRegionRequest(
        @NotBlank String regionCode,
        String regionName,
        String zipFrom,
        String zipTo,
        BigDecimal minWeight,
        BigDecimal maxWeight,
        BigDecimal minVolume,
        BigDecimal maxVolume,
        BigDecimal minOrderAmount,
        @NotNull @DecimalMin(value = "0.00", message = "Frete não pode ser negativo") BigDecimal freightAmount,
        Integer leadTimeDays) {}
