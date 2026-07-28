package br.com.systemcommerce.inventory.mapper;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentReasonResponse;
import br.com.systemcommerce.inventory.dto.InventoryAvailabilityResponse;
import br.com.systemcommerce.inventory.dto.InventoryBalanceResponse;
import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.entity.Inventory;
import br.com.systemcommerce.inventory.entity.InventoryAdjustmentReason;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.service.InventoryBalanceFormulas;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.user.entity.User;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryBalanceResponse toBalance(Inventory inventory) {
        Product product = inventory.getProduct();
        Warehouse warehouse = inventory.getWarehouse();
        Store store = inventory.getStore() != null ? inventory.getStore() : warehouse.getStore();
        BigDecimal physical = InventoryBalanceFormulas.physical(inventory);
        BigDecimal reserved = InventoryBalanceFormulas.reserved(inventory);
        BigDecimal blocked = InventoryBalanceFormulas.blocked(inventory);
        BigDecimal inTransit = InventoryBalanceFormulas.inTransit(inventory);
        BigDecimal available = InventoryBalanceFormulas.available(inventory);
        BigDecimal minStock = inventory.getMinimumQuantity() != null
                ? inventory.getMinimumQuantity()
                : (product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO);
        BigDecimal reorder = inventory.getReorderPoint() != null ? inventory.getReorderPoint() : minStock;
        return new InventoryBalanceResponse(
                inventory.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnitOfMeasure(),
                store != null ? store.getId() : null,
                store != null ? store.getCode() : null,
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                InventoryBalanceFormulas.scale(physical),
                InventoryBalanceFormulas.scale(reserved),
                InventoryBalanceFormulas.scale(blocked),
                InventoryBalanceFormulas.scale(inTransit),
                InventoryBalanceFormulas.scale(available),
                InventoryBalanceFormulas.scale(minStock),
                inventory.getMaximumQuantity() != null
                        ? InventoryBalanceFormulas.scale(inventory.getMaximumQuantity())
                        : null,
                InventoryBalanceFormulas.scale(reorder),
                available.compareTo(minStock) < 0,
                Boolean.TRUE.equals(product.getAllowNegativeStock()),
                inventory.getVersion(),
                inventory.getUpdatedAt());
    }

    public InventoryAvailabilityResponse toAvailability(Inventory inventory, String message) {
        Warehouse warehouse = inventory.getWarehouse();
        Store store = inventory.getStore() != null ? inventory.getStore() : warehouse.getStore();
        BigDecimal available = InventoryBalanceFormulas.available(inventory);
        boolean ok = available.compareTo(BigDecimal.ZERO) > 0;
        return new InventoryAvailabilityResponse(
                inventory.getProduct().getId(),
                store != null ? store.getId() : null,
                warehouse.getId(),
                InventoryBalanceFormulas.scale(available),
                InventoryBalanceFormulas.scale(InventoryBalanceFormulas.physical(inventory)),
                InventoryBalanceFormulas.scale(InventoryBalanceFormulas.reserved(inventory)),
                InventoryBalanceFormulas.scale(InventoryBalanceFormulas.blocked(inventory)),
                InventoryBalanceFormulas.scale(InventoryBalanceFormulas.inTransit(inventory)),
                ok,
                message != null ? message : (ok ? "Disponível" : "Sem saldo disponível"));
    }

    public InventoryAvailabilityResponse emptyAvailability(
            UUID productId, UUID storeId, UUID warehouseId, String message) {
        return new InventoryAvailabilityResponse(
                productId,
                storeId,
                warehouseId,
                BigDecimal.ZERO.setScale(3),
                BigDecimal.ZERO.setScale(3),
                BigDecimal.ZERO.setScale(3),
                BigDecimal.ZERO.setScale(3),
                BigDecimal.ZERO.setScale(3),
                false,
                message);
    }

    public InventoryMovementResponse toMovement(InventoryMovement movement) {
        Product product = movement.getProduct();
        Warehouse warehouse = movement.getWarehouse();
        User user = movement.getUser();
        InventoryAdjustmentReason reason = movement.getAdjustmentReason();
        Store store = warehouse.getStore();
        return new InventoryMovementResponse(
                movement.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                store != null ? store.getId() : null,
                store != null ? store.getCode() : null,
                warehouse.getId(),
                warehouse.getCode(),
                movement.getType(),
                movement.getQuantity(),
                movement.getPreviousQuantity(),
                movement.getNewQuantity(),
                movement.getOrigin(),
                movement.getOriginId(),
                movement.getReason(),
                reason != null ? reason.getId() : null,
                reason != null ? reason.getCode() : null,
                reason != null ? reason.getDescription() : null,
                movement.getObservation(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                movement.getCreatedAt());
    }

    public InventoryAdjustmentReasonResponse toReason(InventoryAdjustmentReason reason) {
        return new InventoryAdjustmentReasonResponse(
                reason.getId(), reason.getCode(), reason.getDescription(), reason.getActive());
    }
}
