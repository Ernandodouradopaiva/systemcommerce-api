package br.com.systemcommerce.salesorder.mapper;

import br.com.systemcommerce.salesorder.dto.SalesOrderItemResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderStatusHistoryResponse;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.entity.SalesOrderItem;
import br.com.systemcommerce.salesorder.entity.SalesOrderStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SalesOrderMapper {

    public SalesOrderResponse toResponse(SalesOrder order) {
        return toResponse(order, order.getItems());
    }

    public SalesOrderResponse toResponse(SalesOrder order, List<SalesOrderItem> items) {
        boolean canGenerate = (order.getStatus() == SalesOrder.SalesOrderStatus.INVOICED
                        || order.getStatus() == SalesOrder.SalesOrderStatus.PICKED)
                && !order.hasGeneratedSale();
        boolean canCancel = order.getStatus() != SalesOrder.SalesOrderStatus.CANCELLED
                && !order.hasGeneratedSale();
        return new SalesOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrganization() != null ? order.getOrganization().getId() : null,
                order.getStore() != null ? order.getStore().getId() : null,
                order.getStore() != null ? order.getStore().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getQuote() != null ? order.getQuote().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                order.getSeller() != null ? order.getSeller().getId() : null,
                order.getSeller() != null ? order.getSeller().getName() : null,
                order.getCarrierName(),
                order.getStatus(),
                order.getNotes(),
                Boolean.TRUE.equals(order.getReserveStock()),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getFreightAmount(),
                order.getTotalAmount(),
                order.getGeneratedSale() != null ? order.getGeneratedSale().getId() : null,
                items == null ? List.of() : items.stream().map(this::toItemResponse).toList(),
                order.isEditable(),
                canCancel,
                canGenerate,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public SalesOrderItemResponse toItemResponse(SalesOrderItem item) {
        return new SalesOrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getLineSubtotal(),
                item.getLineTotal());
    }

    public SalesOrderStatusHistoryResponse toHistoryResponse(SalesOrderStatusHistory history) {
        return new SalesOrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }
}
