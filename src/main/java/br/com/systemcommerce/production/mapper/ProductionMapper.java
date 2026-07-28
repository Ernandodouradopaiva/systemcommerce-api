package br.com.systemcommerce.production.mapper;

import br.com.systemcommerce.production.dto.BillOfMaterialsItemResponse;
import br.com.systemcommerce.production.dto.BillOfMaterialsResponse;
import br.com.systemcommerce.production.dto.ProductionOrderResponse;
import br.com.systemcommerce.production.dto.ProductionOrderStatusHistoryResponse;
import br.com.systemcommerce.production.entity.BillOfMaterials;
import br.com.systemcommerce.production.entity.BillOfMaterialsItem;
import br.com.systemcommerce.production.entity.ProductionOrder;
import br.com.systemcommerce.production.entity.ProductionOrderStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductionMapper {

    public BillOfMaterialsResponse toResponse(BillOfMaterials bom, List<BillOfMaterialsItem> items) {
        return new BillOfMaterialsResponse(
                bom.getId(),
                bom.getOrganization().getId(),
                bom.getFinishedProduct().getId(),
                bom.getFinishedProduct().getSku(),
                bom.getCode(),
                bom.getName(),
                bom.getVersionNumber(),
                bom.getStatus(),
                bom.getNotes(),
                items.stream().map(this::toItemResponse).toList());
    }

    public BillOfMaterialsItemResponse toItemResponse(BillOfMaterialsItem item) {
        return new BillOfMaterialsItemResponse(
                item.getId(),
                item.getComponentProduct().getId(),
                item.getComponentProduct().getSku(),
                item.getQuantity(),
                item.getUnitCode(),
                item.getScrapPercent(),
                item.getLineNumber());
    }

    public ProductionOrderResponse toResponse(ProductionOrder order) {
        return new ProductionOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getStore().getId(),
                order.getWarehouse().getId(),
                order.getBillOfMaterials().getId(),
                order.getFinishedProduct().getId(),
                order.getFinishedProduct().getSku(),
                order.getQuantityPlanned(),
                order.getQuantityCompleted(),
                order.getPlannedStart(),
                order.getPlannedEnd(),
                order.getStartedAt(),
                order.getCompletedAt(),
                order.getUnitCost(),
                order.getTotalCost(),
                order.getNotes());
    }

    public ProductionOrderStatusHistoryResponse toHistoryResponse(ProductionOrderStatusHistory history) {
        return new ProductionOrderStatusHistoryResponse(
                history.getFromStatus(), history.getToStatus(), history.getNotes(), history.getChangedAt());
    }
}
