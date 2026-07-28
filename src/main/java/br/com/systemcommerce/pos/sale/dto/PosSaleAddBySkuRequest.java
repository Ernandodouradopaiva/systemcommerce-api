package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;



public record PosSaleAddBySkuRequest(

        @NotBlank String sku,

        @NotNull @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero") BigDecimal quantity,

        Long expectedVersion) {}


