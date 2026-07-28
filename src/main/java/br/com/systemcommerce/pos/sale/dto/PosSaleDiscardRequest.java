package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;



public record PosSaleDiscardRequest(

        @NotBlank @Size(max = 500) String reason, Long expectedVersion) {}


