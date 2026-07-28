package br.com.systemcommerce.purchase.mapper;

import br.com.systemcommerce.purchase.dto.PurchaseQuotationItemResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationSupplierResponse;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseItemResponse;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseResponse;
import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationItem;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationStatusHistory;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationSupplier;
import br.com.systemcommerce.purchase.entity.SupplierQuotationResponse;
import br.com.systemcommerce.purchase.entity.SupplierQuotationResponseItem;
import br.com.systemcommerce.supplier.entity.Supplier;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PurchaseQuotationMapper {

    private static final Set<PurchaseQuotation.PurchaseQuotationStatus> RESPONSE_ALLOWED = Set.of(
            PurchaseQuotation.PurchaseQuotationStatus.SENT,
            PurchaseQuotation.PurchaseQuotationStatus.RESPONSES_PENDING,
            PurchaseQuotation.PurchaseQuotationStatus.UNDER_COMPARISON);

    public PurchaseQuotationResponse toResponse(PurchaseQuotation quotation) {
        boolean canEdit = quotation.isEditable();
        boolean canSend = quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.DRAFT
                || quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.OPEN;
        boolean canRegisterResponse = RESPONSE_ALLOWED.contains(quotation.getStatus());
        boolean canSelectItems = RESPONSE_ALLOWED.contains(quotation.getStatus())
                || quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED;
        boolean canGeneratePurchaseOrders =
                quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.SELECTED
                        || quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED;
        boolean canClose = canGeneratePurchaseOrders;
        boolean canCancel = !quotation.isLocked();
        return new PurchaseQuotationResponse(
                quotation.getId(),
                quotation.getQuotationNumber(),
                quotation.getOrganization() != null ? quotation.getOrganization().getId() : null,
                quotation.getStore() != null ? quotation.getStore().getId() : null,
                quotation.getStore() != null ? quotation.getStore().getCode() : null,
                quotation.getPurchaseRequest() != null ? quotation.getPurchaseRequest().getId() : null,
                quotation.getPurchaseRequest() != null ? quotation.getPurchaseRequest().getRequestNumber() : null,
                quotation.getBuyer() != null ? quotation.getBuyer().getId() : null,
                quotation.getBuyer() != null ? quotation.getBuyer().getName() : null,
                quotation.getOpenedAt(),
                quotation.getResponseDeadline(),
                quotation.getStatus(),
                quotation.getSelectionCriteria(),
                quotation.getAutoSelectLowestPrice(),
                quotation.getNotes(),
                quotation.getClosedAt(),
                quotation.getItems() == null
                        ? List.of()
                        : quotation.getItems().stream().map(this::toItemResponse).toList(),
                quotation.getSuppliers() == null
                        ? List.of()
                        : quotation.getSuppliers().stream().map(this::toSupplierResponse).toList(),
                canEdit,
                canSend,
                canRegisterResponse,
                canSelectItems,
                canGeneratePurchaseOrders,
                canClose,
                canCancel,
                quotation.getVersion(),
                quotation.getCreatedAt(),
                quotation.getUpdatedAt());
    }

    public PurchaseQuotationItemResponse toItemResponse(PurchaseQuotationItem item) {
        return new PurchaseQuotationItemResponse(
                item.getId(),
                item.getPurchaseRequestItem() != null ? item.getPurchaseRequestItem().getId() : null,
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnit(),
                item.getQuantitySelected(),
                item.pendingSelection());
    }

    public PurchaseQuotationSupplierResponse toSupplierResponse(PurchaseQuotationSupplier supplier) {
        return new PurchaseQuotationSupplierResponse(
                supplier.getId(),
                supplier.getSupplier().getId(),
                displayName(supplier.getSupplier()),
                supplier.getInvitedAt(),
                supplier.getStatus(),
                supplier.getNotes(),
                supplier.getStatus() != PurchaseQuotationSupplier.InviteStatus.INVITED);
    }

    public PurchaseQuotationStatusHistoryResponse toHistoryResponse(PurchaseQuotationStatusHistory history) {
        return new PurchaseQuotationStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }

    public SupplierQuotationResponseResponse toResponseResponse(SupplierQuotationResponse response) {
        return new SupplierQuotationResponseResponse(
                response.getId(),
                response.getSupplier().getId(),
                displayName(response.getSupplier()),
                response.getPaymentCondition(),
                response.getFreightAmount(),
                response.getTaxAmount(),
                response.getDiscountAmount(),
                response.getLeadTimeDays(),
                response.getValidUntil(),
                response.getNotes(),
                response.getTotalAmount(),
                response.getSubmittedAt(),
                Boolean.TRUE.equals(response.getLocked()),
                response.getItems() == null
                        ? List.of()
                        : response.getItems().stream().map(this::toResponseItemResponse).toList());
    }

    public SupplierQuotationResponseItemResponse toResponseItemResponse(SupplierQuotationResponseItem item) {
        return new SupplierQuotationResponseItemResponse(
                item.getId(),
                item.getQuotationItem().getId(),
                item.getUnitPrice(),
                item.getQuantityAvailable(),
                item.getFreightAmount(),
                item.getTaxAmount(),
                item.getDiscountAmount(),
                item.getLeadTimeDays(),
                item.getBrandOffered(),
                item.getLineTotal(),
                Boolean.TRUE.equals(item.getSelected()),
                item.getQuantitySelected(),
                item.getNotes());
    }

    private static String displayName(Supplier supplier) {
        if (supplier.getTradeName() != null && !supplier.getTradeName().isBlank()) {
            return supplier.getTradeName();
        }
        return supplier.getLegalName();
    }
}
