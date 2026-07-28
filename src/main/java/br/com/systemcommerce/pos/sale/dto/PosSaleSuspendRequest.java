package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.Size;



public record PosSaleSuspendRequest(

        @Size(max = 500) String reason, Long expectedVersion) {}


