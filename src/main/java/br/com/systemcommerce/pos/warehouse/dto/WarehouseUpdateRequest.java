package br.com.systemcommerce.pos.warehouse.dto;

import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        Boolean allowsSale,
        Warehouse.WarehouseType warehouseType,
        Boolean central,
        Boolean virtualWarehouse,
        Boolean blockedForMovement) {}
