package br.com.systemcommerce.pos.warehouse.dto;

import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record WarehouseCreateRequest(
        @NotNull(message = "loja é obrigatória") UUID storeId,
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        Boolean allowsSale,
        Warehouse.WarehouseType warehouseType,
        Boolean central,
        Boolean virtualWarehouse,
        Boolean blockedForMovement) {

    public WarehouseCreateRequest(UUID storeId, String code, String name, Boolean allowsSale) {
        this(storeId, code, name, allowsSale, null, null, null, null);
    }
}
