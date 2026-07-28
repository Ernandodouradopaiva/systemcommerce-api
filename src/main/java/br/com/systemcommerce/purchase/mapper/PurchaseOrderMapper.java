package br.com.systemcommerce.purchase.mapper;

import br.com.systemcommerce.purchase.dto.PurchaseOrderItemResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderStatusHistoryResponse;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseOrderStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder order) {
        return toResponse(order, order.getItems());
    }

    public PurchaseOrderResponse toResponse(PurchaseOrder order, List<PurchaseOrderItem> items) {
        boolean canEdit = order.isEditable();
        boolean canRevise = order.isRevisable();
        boolean canCancel = order.isCancellable();
        boolean canSend = order.getStatus() == PurchaseOrder.PurchaseOrderStatus.DRAFT;
        boolean canApprove = order.isApprovableNow();
        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrganization() != null ? order.getOrganization().getId() : null,
                order.getStore() != null ? order.getStore().getId() : null,
                order.getStore() != null ? order.getStore().getCode() : null,
                order.getDestinationStore() != null ? order.getDestinationStore().getId() : null,
                order.getDestinationStore() != null ? order.getDestinationStore().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getSupplier() != null ? order.getSupplier().getId() : null,
                order.getSupplier() != null ? supplierDisplayName(order.getSupplier()) : null,
                order.getBuyer() != null ? order.getBuyer().getId() : null,
                order.getBuyer() != null ? order.getBuyer().getName() : null,
                order.getPurchaseQuotation() != null ? order.getPurchaseQuotation().getId() : null,
                order.getPurchaseQuotation() != null ? order.getPurchaseQuotation().getQuotationNumber() : null,
                order.getStatus(),
                order.getExpectedDate(),
                order.getIssuedAt(),
                order.getNotes(),
                order.getPaymentCondition(),
                order.getCarrierName(),
                order.getFreightModality(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getFreightAmount(),
                order.getTaxAmount(),
                order.getInsuranceAmount(),
                order.getExpenseAmount(),
                order.getTotalAmount(),
                order.getRevisionNumber(),
                order.getApprovalRequired(),
                order.getApprovalThresholdAmount(),
                order.getAllowOverReceipt(),
                items == null ? List.of() : items.stream().map(this::toItemResponse).toList(),
                canEdit,
                canRevise,
                canCancel,
                canSend,
                canApprove,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getDescription(),
                item.getQuantityOrdered(),
                item.getQuantityReceived(),
                item.getQuantityCancelled(),
                item.getUnitCost(),
                item.getDiscountAmount(),
                item.getTaxAmount(),
                item.getLineTotal(),
                item.getExpectedDate());
    }

    public PurchaseOrderStatusHistoryResponse toHistoryResponse(PurchaseOrderStatusHistory history) {
        return new PurchaseOrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }

    private static String supplierDisplayName(br.com.systemcommerce.supplier.entity.Supplier supplier) {
        if (supplier.getTradeName() != null && !supplier.getTradeName().isBlank()) {
            return supplier.getTradeName();
        }
        return supplier.getLegalName();
    }
}
