package br.com.systemcommerce.reservation.mapper;

import br.com.systemcommerce.reservation.dto.StockReservationItemResponse;
import br.com.systemcommerce.reservation.dto.StockReservationResponse;
import br.com.systemcommerce.reservation.dto.StockReservationStatusHistoryResponse;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.entity.StockReservationItem;
import br.com.systemcommerce.reservation.entity.StockReservationStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class StockReservationMapper {

    public StockReservationResponse toResponse(StockReservation reservation) {
        return new StockReservationResponse(
                reservation.getId(),
                reservation.getReservationNumber(),
                reservation.getOrganization() != null ? reservation.getOrganization().getId() : null,
                reservation.getStore() != null ? reservation.getStore().getId() : null,
                reservation.getStore() != null ? reservation.getStore().getCode() : null,
                reservation.getWarehouse() != null ? reservation.getWarehouse().getId() : null,
                reservation.getWarehouse() != null ? reservation.getWarehouse().getCode() : null,
                reservation.getOriginType(),
                reservation.getOriginId(),
                reservation.getOriginNumber(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.getNotes(),
                reservation.getItems().stream().map(this::toItemResponse).toList(),
                reservation.getVersion(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    public StockReservationItemResponse toItemResponse(StockReservationItem item) {
        return new StockReservationItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getSku() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getQuantityReserved(),
                item.getQuantityConsumed(),
                item.getQuantityReleased(),
                item.remaining());
    }

    public StockReservationStatusHistoryResponse toHistoryResponse(StockReservationStatusHistory history) {
        return new StockReservationStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy());
    }
}
