package br.com.systemcommerce.stocktransfer.mapper;

import br.com.systemcommerce.stocktransfer.dto.StockTransferInTransitItemResponse;
import br.com.systemcommerce.stocktransfer.dto.StockTransferItemResponse;
import br.com.systemcommerce.stocktransfer.dto.StockTransferResponse;
import br.com.systemcommerce.stocktransfer.entity.StockTransfer;
import br.com.systemcommerce.stocktransfer.entity.StockTransferItem;
import br.com.systemcommerce.stocktransfer.repository.StockTransferItemRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockTransferMapper {

    private final StockTransferItemRepository itemRepository;

    public StockTransferResponse toResponse(StockTransfer transfer) {
        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transfer.getId());
        return new StockTransferResponse(
                transfer.getId(),
                transfer.getOrganization().getId(),
                transfer.getNumber(),
                transfer.getOriginStore().getId(),
                transfer.getOriginStore().getCode(),
                transfer.getOriginWarehouse().getId(),
                transfer.getOriginWarehouse().getCode(),
                transfer.getDestinationStore().getId(),
                transfer.getDestinationStore().getCode(),
                transfer.getDestinationWarehouse().getId(),
                transfer.getDestinationWarehouse().getCode(),
                transfer.getRequester() != null ? transfer.getRequester().getId() : null,
                transfer.getApprover() != null ? transfer.getApprover().getId() : null,
                transfer.getDispatcher() != null ? transfer.getDispatcher().getId() : null,
                transfer.getReceiver() != null ? transfer.getReceiver().getId() : null,
                transfer.getRequestedAt(),
                transfer.getDispatchedAt(),
                transfer.getReceivedAt(),
                transfer.getStatus(),
                transfer.getObservation(),
                transfer.getReason(),
                items.stream().map(this::toItemResponse).toList(),
                transfer.getVersion(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt());
    }

    public StockTransferItemResponse toItemResponse(StockTransferItem item) {
        BigDecimal dispatched = defaultZero(item.getQuantityDispatched());
        BigDecimal received = defaultZero(item.getQuantityReceived());
        BigDecimal divergent = defaultZero(item.getQuantityDivergent());
        return new StockTransferItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getProduct().getUnitOfMeasure(),
                item.getQuantityRequested(),
                item.getQuantityApproved(),
                dispatched,
                received,
                divergent,
                dispatched.subtract(received).subtract(divergent),
                item.getObservation(),
                item.getVersion());
    }

    public StockTransferInTransitItemResponse toInTransitItemResponse(StockTransferItem item) {
        StockTransfer transfer = item.getTransfer();
        BigDecimal dispatched = defaultZero(item.getQuantityDispatched());
        BigDecimal received = defaultZero(item.getQuantityReceived());
        BigDecimal divergent = defaultZero(item.getQuantityDivergent());
        return new StockTransferInTransitItemResponse(
                transfer.getId(),
                transfer.getNumber(),
                transfer.getStatus(),
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                transfer.getOriginStore().getId(),
                transfer.getOriginStore().getCode(),
                transfer.getDestinationStore().getId(),
                transfer.getDestinationStore().getCode(),
                dispatched,
                received,
                dispatched.subtract(received).subtract(divergent),
                transfer.getDispatchedAt());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
