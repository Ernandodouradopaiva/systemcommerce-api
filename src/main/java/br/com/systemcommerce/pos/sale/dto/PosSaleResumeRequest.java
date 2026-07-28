package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.NotNull;

import java.util.UUID;



public record PosSaleResumeRequest(

        @NotNull(message = "cashSessionId é obrigatório") UUID cashSessionId, Long expectedVersion) {}


