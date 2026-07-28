package br.com.systemcommerce.pos.cancellation.mapper;

import br.com.systemcommerce.pos.cancellation.dto.CancellationRefundResponse;
import br.com.systemcommerce.pos.cancellation.dto.SaleCancellationResponse;
import br.com.systemcommerce.pos.cancellation.dto.SaleReturnResponse;
import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturn;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturnItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PosCancellationMapper {

    public SaleCancellationResponse toResponse(SaleCancellation cancellation) {
        List<CancellationRefundResponse> refunds = cancellation.getRefunds() == null
                ? List.of()
                : cancellation.getRefunds().stream().map(this::toRefundResponse).toList();
        return new SaleCancellationResponse(
                cancellation.getId(),
                cancellation.getSale().getId(),
                cancellation.getSale().getSaleNumber(),
                cancellation.getSale().getStatus(),
                cancellation.getStatus(),
                cancellation.getReason(),
                cancellation.getRequestedBy() != null ? cancellation.getRequestedBy().getId() : null,
                userName(cancellation.getRequestedBy()),
                cancellation.getAuthorizedBy() != null ? cancellation.getAuthorizedBy().getId() : null,
                userName(cancellation.getAuthorizedBy()),
                cancellation.getExecutedBy() != null ? cancellation.getExecutedBy().getId() : null,
                userName(cancellation.getExecutedBy()),
                cancellation.getRequestedAt(),
                cancellation.getAuthorizedAt(),
                cancellation.getExecutedAt(),
                cancellation.getDecisionNotes(),
                cancellation.getFailureDetail(),
                refunds,
                cancellation.getVersion());
    }

    public CancellationRefundResponse toRefundResponse(CancellationRefund refund) {
        return new CancellationRefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getStatus(),
                refund.getMethod(),
                refund.getAmount(),
                refund.getFailureReason(),
                refund.getAttempts(),
                refund.getLastAttemptAt(),
                refund.getCompletedAt());
    }

    public SaleReturnResponse toReturnResponse(SaleReturn saleReturn) {
        List<SaleReturnResponse.SaleReturnItemResponse> items = saleReturn.getItems() == null
                ? List.of()
                : saleReturn.getItems().stream().map(this::toReturnItem).toList();
        return new SaleReturnResponse(
                saleReturn.getId(),
                saleReturn.getReturnNumber(),
                saleReturn.getOriginalSale().getId(),
                saleReturn.getOriginalSale().getSaleNumber(),
                saleReturn.getCashSession() != null ? saleReturn.getCashSession().getId() : null,
                saleReturn.getStatus(),
                saleReturn.getReason(),
                saleReturn.getNotes(),
                saleReturn.getRequestedBy() != null ? saleReturn.getRequestedBy().getId() : null,
                userName(saleReturn.getRequestedBy()),
                saleReturn.getConfirmedAt(),
                items,
                saleReturn.getVersion());
    }

    private SaleReturnResponse.SaleReturnItemResponse toReturnItem(SaleReturnItem item) {
        return new SaleReturnResponse.SaleReturnItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getOriginalSaleItem() != null ? item.getOriginalSaleItem().getId() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }

    private String userName(br.com.systemcommerce.user.entity.User user) {
        if (user == null) {
            return null;
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getLogin();
    }
}
