package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

import java.util.UUID;



public record PosSaleItemDiscountRequest(

        @NotNull @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,

        Long expectedVersion,

        UUID authorizedById) {}


