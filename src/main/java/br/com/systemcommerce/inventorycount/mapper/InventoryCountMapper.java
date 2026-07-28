package br.com.systemcommerce.inventorycount.mapper;

import br.com.systemcommerce.inventorycount.dto.InventoryCountItemResponse;
import br.com.systemcommerce.inventorycount.dto.InventoryCountResponse;
import br.com.systemcommerce.inventorycount.dto.InventoryCountStatusHistoryResponse;
import br.com.systemcommerce.inventorycount.entity.InventoryCount;
import br.com.systemcommerce.inventorycount.entity.InventoryCountItem;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryCountMapper {

    public InventoryCountResponse toResponse(InventoryCount count, List<InventoryCountItem> items) {
        return new InventoryCountResponse(
                count.getId(),
                count.getCountNumber(),
                count.getCountType(),
                count.getStatus(),
                count.getStore().getId(),
                count.getStore().getName(),
                count.getWarehouse().getId(),
                count.getWarehouse().getCode(),
                count.getFreezeBalances(),
                count.getHideTheoreticalQty(),
                count.getRequireSecondCount(),
                count.getPlannedAt(),
                count.getOpenedAt(),
                count.getClosedAt(),
                count.getPostedAt(),
                count.getNotes(),
                items.stream().map(this::toItemResponse).toList());
    }

    public InventoryCountItemResponse toItemResponse(InventoryCountItem item) {
        return new InventoryCountItemResponse(
                item.getId(),
                item.getLineNumber(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getStorageLocation() != null ? item.getStorageLocation().getId() : null,
                item.getStorageLocation() != null ? item.getStorageLocation().getCode() : null,
                item.getTheoreticalQuantity(),
                item.getCountedQuantity1(),
                item.getCountedQuantity2(),
                item.getFinalCountedQuantity(),
                item.getVarianceQuantity(),
                item.getUnitCost(),
                item.getFrozen(),
                item.getNotes());
    }

    public InventoryCountStatusHistoryResponse toHistoryResponse(InventoryCountStatusHistory history) {
        return new InventoryCountStatusHistoryResponse(
                history.getFromStatus(), history.getToStatus(), history.getNotes(), history.getChangedAt());
    }
}
