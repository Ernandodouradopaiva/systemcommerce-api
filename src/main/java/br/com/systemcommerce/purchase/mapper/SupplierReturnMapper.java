package br.com.systemcommerce.purchase.mapper;

import br.com.systemcommerce.purchase.dto.SupplierReturnItemResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnStatusHistoryResponse;
import br.com.systemcommerce.purchase.entity.SupplierReturn;
import br.com.systemcommerce.purchase.entity.SupplierReturnItem;
import br.com.systemcommerce.purchase.entity.SupplierReturnStatusHistory;
import br.com.systemcommerce.supplier.entity.Supplier;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SupplierReturnMapper {

    private static final Set<SupplierReturn.SupplierReturnStatus> CANCELLABLE = EnumSet.of(
            SupplierReturn.SupplierReturnStatus.DRAFT,
            SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL,
            SupplierReturn.SupplierReturnStatus.APPROVED);

    public SupplierReturnResponse toResponse(SupplierReturn supplierReturn) {
        SupplierReturn.SupplierReturnStatus status = supplierReturn.getStatus();
        boolean canEdit = status == SupplierReturn.SupplierReturnStatus.DRAFT;
        boolean canSubmit = status == SupplierReturn.SupplierReturnStatus.DRAFT;
        boolean canApprove = status == SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL;
        boolean canReject = status == SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL;
        boolean canDispatch = status == SupplierReturn.SupplierReturnStatus.APPROVED;
        boolean canComplete = status == SupplierReturn.SupplierReturnStatus.DISPATCHED;
        boolean canCancel = CANCELLABLE.contains(status);
        return new SupplierReturnResponse(
                supplierReturn.getId(),
                supplierReturn.getReturnNumber(),
                supplierReturn.getOrganization() != null ? supplierReturn.getOrganization().getId() : null,
                supplierReturn.getStore() != null ? supplierReturn.getStore().getId() : null,
                supplierReturn.getStore() != null ? supplierReturn.getStore().getCode() : null,
                supplierReturn.getWarehouse() != null ? supplierReturn.getWarehouse().getId() : null,
                supplierReturn.getWarehouse() != null ? supplierReturn.getWarehouse().getCode() : null,
                supplierReturn.getSupplier() != null ? supplierReturn.getSupplier().getId() : null,
                supplierReturn.getSupplier() != null ? displayName(supplierReturn.getSupplier()) : null,
                supplierReturn.getPurchaseOrder() != null ? supplierReturn.getPurchaseOrder().getId() : null,
                supplierReturn.getPurchaseOrder() != null ? supplierReturn.getPurchaseOrder().getOrderNumber() : null,
                supplierReturn.getPurchaseReceipt() != null ? supplierReturn.getPurchaseReceipt().getId() : null,
                supplierReturn.getPurchaseReceipt() != null
                        ? supplierReturn.getPurchaseReceipt().getReceiptNumber()
                        : null,
                supplierReturn.getReason(),
                supplierReturn.getReasonNotes(),
                status,
                supplierReturn.getOriginType(),
                supplierReturn.getDispatchedAt(),
                supplierReturn.getCompletedAt(),
                supplierReturn.getNotes(),
                supplierReturn.getItems() == null
                        ? List.of()
                        : supplierReturn.getItems().stream().map(this::toItemResponse).toList(),
                canEdit,
                canSubmit,
                canApprove,
                canReject,
                canDispatch,
                canComplete,
                canCancel,
                supplierReturn.getVersion(),
                supplierReturn.getCreatedAt(),
                supplierReturn.getUpdatedAt());
    }

    public SupplierReturnItemResponse toItemResponse(SupplierReturnItem item) {
        return new SupplierReturnItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getPurchaseOrderItem() != null ? item.getPurchaseOrderItem().getId() : null,
                item.getPurchaseReceiptItem() != null ? item.getPurchaseReceiptItem().getId() : null,
                item.getLineNumber(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getBatchCode(),
                item.getExpiryDate(),
                item.getSerialNumber(),
                item.getNotes());
    }

    public SupplierReturnStatusHistoryResponse toHistoryResponse(SupplierReturnStatusHistory history) {
        return new SupplierReturnStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }

    private static String displayName(Supplier supplier) {
        if (supplier.getTradeName() != null && !supplier.getTradeName().isBlank()) {
            return supplier.getTradeName();
        }
        return supplier.getLegalName();
    }
}
