package br.com.systemcommerce.pos.warehouse.mapper;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseUpdateRequest;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseResponse toResponse(Warehouse warehouse) {
        Store store = warehouse.getStore();
        return new WarehouseResponse(
                warehouse.getId(),
                store.getId(),
                store.getCode(),
                store.getName(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAllowsSale(),
                warehouse.getStatus(),
                warehouse.getWarehouseType(),
                warehouse.getCentral(),
                warehouse.getVirtualWarehouse(),
                warehouse.getBlockedForMovement(),
                warehouse.getActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt());
    }

    public void applyCreate(Warehouse warehouse, WarehouseCreateRequest request, Store store) {
        warehouse.setStore(store);
        warehouse.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        warehouse.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        warehouse.setAllowsSale(request.allowsSale() == null || request.allowsSale());
        warehouse.setWarehouseType(request.warehouseType() != null ? request.warehouseType() : Warehouse.WarehouseType.SALE);
        warehouse.setCentral(Boolean.TRUE.equals(request.central()));
        warehouse.setVirtualWarehouse(Boolean.TRUE.equals(request.virtualWarehouse()));
        warehouse.setBlockedForMovement(Boolean.TRUE.equals(request.blockedForMovement()));
        warehouse.markActive();
    }

    public void applyUpdate(Warehouse warehouse, WarehouseUpdateRequest request) {
        warehouse.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        warehouse.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        if (request.allowsSale() != null) {
            warehouse.setAllowsSale(request.allowsSale());
        }
        if (request.warehouseType() != null) {
            warehouse.setWarehouseType(request.warehouseType());
        }
        if (request.central() != null) {
            warehouse.setCentral(request.central());
        }
        if (request.virtualWarehouse() != null) {
            warehouse.setVirtualWarehouse(request.virtualWarehouse());
        }
        if (request.blockedForMovement() != null) {
            warehouse.setBlockedForMovement(request.blockedForMovement());
        }
    }
}
