package br.com.systemcommerce.picking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Bipagem de item na separação (Prompt 71) — por código de barras + quantidade. Idempotente via
 * {@code idempotencyKey} (ex.: identificador único de cada bipagem do app mobile).
 */
public record PickingItemPickRequest(
        @NotNull String barcode,
        @NotNull @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero") BigDecimal quantity,
        String idempotencyKey) {}
