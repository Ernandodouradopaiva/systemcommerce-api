package br.com.systemcommerce.pos.terminal.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PosTerminalLinkWarehouseRequest(
        @NotNull(message = "depósito é obrigatório") UUID warehouseId) {}
