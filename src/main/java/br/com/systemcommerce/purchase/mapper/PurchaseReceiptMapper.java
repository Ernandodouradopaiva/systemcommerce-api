package br.com.systemcommerce.purchase.mapper;

import br.com.systemcommerce.purchase.dto.PurchaseReceiptDivergenceResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptItemResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptStatusHistoryResponse;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptDivergence;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseReceiptMapper {

    public PurchaseReceiptResponse toResponse(PurchaseReceipt receipt) {
        return toResponse(receipt, receipt.getItems());
    }

    public PurchaseReceiptResponse toResponse(PurchaseReceipt receipt, List<PurchaseReceiptItem> items) {
        return new PurchaseReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getOrganization() != null ? receipt.getOrganization().getId() : null,
                receipt.getStore() != null ? receipt.getStore().getId() : null,
                receipt.getStore() != null ? receipt.getStore().getCode() : null,
                receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null,
                receipt.getWarehouse() != null ? receipt.getWarehouse().getCode() : null,
                receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getId() : null,
                receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getOrderNumber() : null,
                receipt.getSupplier() != null ? receipt.getSupplier().getId() : null,
                receipt.getSupplier() != null ? supplierDisplayName(receipt.getSupplier()) : null,
                receipt.getReceiptDate(),
                receipt.getInvoiceNumber(),
                receipt.getInvoiceSeries(),
                receipt.getAccessKey(),
                receipt.getInvoiceIssuedAt(),
                receipt.getCarrierName(),
                receipt.getNotes(),
                receipt.getStatus(),
                receipt.getReceivedBy() != null ? receipt.getReceivedBy().getId() : null,
                receipt.getReceivedBy() != null ? receipt.getReceivedBy().getName() : null,
                receipt.getPostedAt(),
                receipt.getPostedBy() != null ? receipt.getPostedBy().getId() : null,
                items == null ? List.of() : items.stream().map(this::toItemResponse).toList(),
                receipt.isInspectable(),
                receipt.isAcceptable(),
                receipt.isPostable(),
                receipt.isInspectable(),
                receipt.isCancellable(),
                receipt.getVersion(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt());
    }

    public PurchaseReceiptItemResponse toItemResponse(PurchaseReceiptItem item) {
        return new PurchaseReceiptItemResponse(
                item.getId(),
                item.getPurchaseOrderItem() != null ? item.getPurchaseOrderItem().getId() : null,
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getQuantityOrdered(),
                item.getQuantityPreviouslyReceived(),
                item.getQuantityReceived(),
                item.getQuantityRejected(),
                item.getQuantityAccepted(),
                item.getQuantityDivergent(),
                item.getUnitCost(),
                item.getBatchCode(),
                item.getExpiryDate(),
                item.getSerialNumber(),
                item.getDestinationLocation());
    }

    public PurchaseReceiptStatusHistoryResponse toHistoryResponse(PurchaseReceiptStatusHistory history) {
        return new PurchaseReceiptStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }

    public PurchaseReceiptDivergenceResponse toDivergenceResponse(PurchaseReceiptDivergence divergence) {
        return new PurchaseReceiptDivergenceResponse(
                divergence.getId(),
                divergence.getPurchaseReceiptItem() != null ? divergence.getPurchaseReceiptItem().getId() : null,
                divergence.getDivergenceType(),
                divergence.getDescription(),
                divergence.getQuantity(),
                divergence.getCreatedAt());
    }

    private static String supplierDisplayName(br.com.systemcommerce.supplier.entity.Supplier supplier) {
        if (supplier.getTradeName() != null && !supplier.getTradeName().isBlank()) {
            return supplier.getTradeName();
        }
        return supplier.getLegalName();
    }
}
