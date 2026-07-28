package br.com.systemcommerce.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Linha genérica de consumo/liberação parcial por produto, usada por integrações (picking, faturamento). */
public record StockReservationLineRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero") BigDecimal quantity) {}
