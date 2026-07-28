package br.com.systemcommerce.pos.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRackRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code, @Size(max = 120) String name) {}
