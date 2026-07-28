package br.com.systemcommerce.purchase.mapper;

import br.com.systemcommerce.purchase.dto.PurchaseRequestItemResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestStatusHistoryResponse;
import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequestItem;
import br.com.systemcommerce.purchase.entity.PurchaseRequestStatusHistory;
import br.com.systemcommerce.supplier.entity.Supplier;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestMapper {

    public PurchaseRequestResponse toResponse(PurchaseRequest request) {
        return toResponse(request, request.getItems());
    }

    public PurchaseRequestResponse toResponse(PurchaseRequest request, List<PurchaseRequestItem> items) {
        return new PurchaseRequestResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getOrganization() != null ? request.getOrganization().getId() : null,
                request.getStore() != null ? request.getStore().getId() : null,
                request.getStore() != null ? request.getStore().getCode() : null,
                request.getWarehouse() != null ? request.getWarehouse().getId() : null,
                request.getWarehouse() != null ? request.getWarehouse().getCode() : null,
                request.getRequestingSector(),
                request.getRequester() != null ? request.getRequester().getId() : null,
                request.getRequester() != null ? request.getRequester().getName() : null,
                request.getBuyer() != null ? request.getBuyer().getId() : null,
                request.getBuyer() != null ? request.getBuyer().getName() : null,
                request.getPriority(),
                request.getRequestedAt(),
                request.getDesiredDate(),
                request.getJustification(),
                request.getNotes(),
                request.getStatus(),
                request.getRequiresApproval(),
                request.getRejectionReason(),
                request.getCancellationReason(),
                items == null ? List.of() : items.stream().map(this::toItemResponse).toList(),
                request.isEditable(),
                request.getStatus() == PurchaseRequest.PurchaseRequestStatus.DRAFT,
                request.getStatus() == PurchaseRequest.PurchaseRequestStatus.SUBMITTED,
                request.getStatus() == PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS,
                request.getStatus() == PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS,
                request.isCancellable(),
                request.isConvertible(),
                request.getVersion(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public PurchaseRequestItemResponse toItemResponse(PurchaseRequestItem item) {
        Supplier suggested = item.getSuggestedSupplier();
        return new PurchaseRequestItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getDescription(),
                item.getQuantityRequested(),
                item.getQuantityApproved(),
                item.getQuantityConverted(),
                item.pendingQuantity(),
                item.getUnit(),
                item.getCurrentStockInfo(),
                item.getMinimumStock(),
                item.getJustification(),
                suggested != null ? suggested.getId() : null,
                suggested != null ? displayName(suggested) : null);
    }

    public PurchaseRequestStatusHistoryResponse toHistoryResponse(PurchaseRequestStatusHistory history) {
        return new PurchaseRequestStatusHistoryResponse(
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
