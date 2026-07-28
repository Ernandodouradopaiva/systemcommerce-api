package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;



public record PosSaleSurchargeRequest(

        @NotNull @DecimalMin(value = "0.00", message = "Acréscimo não pode ser negativo") BigDecimal surchargeAmount,

        Long expectedVersion) {}


