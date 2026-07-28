package br.com.systemcommerce.picking.mapper;

import br.com.systemcommerce.picking.dto.PickingOrderItemResponse;
import br.com.systemcommerce.picking.dto.PickingOrderPrintDataResponse;
import br.com.systemcommerce.picking.dto.PickingOrderResponse;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.picking.entity.PickingOrderItem;
import br.com.systemcommerce.picking.repository.PickingOrderItemRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickingOrderMapper {

    private final PickingOrderItemRepository pickingOrderItemRepository;

    public PickingOrderResponse toResponse(PickingOrder order) {
        boolean canStart = order.getStatus() == PickingOrder.PickingOrderStatus.PENDING
                || order.getStatus() == PickingOrder.PickingOrderStatus.ASSIGNED;
        boolean canComplete = order.getStatus() == PickingOrder.PickingOrderStatus.IN_PROGRESS
                || order.getStatus() == PickingOrder.PickingOrderStatus.PARTIALLY_PICKED
                || order.getStatus() == PickingOrder.PickingOrderStatus.DIVERGENT;
        return new PickingOrderResponse(
                order.getId(),
                order.getPickingNumber(),
                order.getOrganization() != null ? order.getOrganization().getId() : null,
                order.getStore() != null ? order.getStore().getId() : null,
                order.getStore() != null ? order.getStore().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getSalesOrder() != null ? order.getSalesOrder().getId() : null,
                order.getSalesOrder() != null ? order.getSalesOrder().getOrderNumber() : null,
                order.getStockReservation() != null ? order.getStockReservation().getId() : null,
                order.getStatus(),
                order.getAssignedTo() != null ? order.getAssignedTo().getId() : null,
                order.getAssignedTo() != null ? order.getAssignedTo().getName() : null,
                order.getStartedAt(),
                order.getCompletedAt(),
                order.getNotes(),
                order.getItems().stream().map(item -> toItemResponse(item, null)).toList(),
                canStart,
                canComplete,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public PickingOrderItemResponse toItemResponse(PickingOrderItem item, String storageLocationCode) {
        String code = storageLocationCode;
        if (code == null && item.getStorageLocationId() != null) {
            code = pickingOrderItemRepository.findStorageLocationCode(item.getStorageLocationId());
        }
        return new PickingOrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getSku() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getProduct() != null ? item.getProduct().getBarcode() : null,
                item.getStorageLocationId(),
                code,
                item.getLineNumber(),
                item.getQuantityRequested(),
                item.getQuantityPicked(),
                item.pending(),
                item.getBarcodeScanned(),
                item.getSubstituteProduct() != null ? item.getSubstituteProduct().getId() : null,
                item.getNotes());
    }

    /** Monta o DTO mobile-friendly com itens já ordenados por localização física. */
    public PickingOrderPrintDataResponse toPrintData(PickingOrder order) {
        List<UUID> orderedIds = pickingOrderItemRepository.findIdsOrderedByStorageLocationCode(order.getId());
        Map<UUID, PickingOrderItem> byId =
                order.getItems().stream().collect(java.util.stream.Collectors.toMap(PickingOrderItem::getId, i -> i));
        List<PickingOrderItem> ordered = orderedIds.isEmpty()
                ? order.getItems().stream()
                        .sorted(Comparator.comparing(PickingOrderItem::getLineNumber))
                        .toList()
                : orderedIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();

        List<PickingOrderPrintDataResponse.PrintLine> lines = ordered.stream()
                .map(item -> new PickingOrderPrintDataResponse.PrintLine(
                        item.getId(),
                        item.getStorageLocationId() != null
                                ? pickingOrderItemRepository.findStorageLocationCode(item.getStorageLocationId())
                                : null,
                        item.getProduct() != null ? item.getProduct().getSku() : null,
                        item.getProduct() != null ? item.getProduct().getName() : null,
                        item.getProduct() != null ? item.getProduct().getBarcode() : null,
                        item.getQuantityRequested(),
                        item.pending()))
                .toList();

        return new PickingOrderPrintDataResponse(
                order.getId(),
                order.getPickingNumber(),
                order.getStore() != null ? order.getStore().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getSalesOrder() != null ? order.getSalesOrder().getOrderNumber() : null,
                order.getStatus().name(),
                lines);
    }
}
