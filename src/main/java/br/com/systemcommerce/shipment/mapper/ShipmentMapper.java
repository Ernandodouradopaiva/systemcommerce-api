package br.com.systemcommerce.shipment.mapper;

import br.com.systemcommerce.shipment.dto.DeliveryProofResponse;
import br.com.systemcommerce.shipment.dto.ShipmentItemResponse;
import br.com.systemcommerce.shipment.dto.ShipmentPackageResponse;
import br.com.systemcommerce.shipment.dto.ShipmentResponse;
import br.com.systemcommerce.shipment.dto.ShipmentTrackingResponse;
import br.com.systemcommerce.shipment.entity.DeliveryProof;
import br.com.systemcommerce.shipment.entity.Shipment;
import br.com.systemcommerce.shipment.entity.ShipmentItem;
import br.com.systemcommerce.shipment.entity.ShipmentPackage;
import br.com.systemcommerce.shipment.entity.ShipmentTracking;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrganization() != null ? shipment.getOrganization().getId() : null,
                shipment.getStore() != null ? shipment.getStore().getId() : null,
                shipment.getWarehouse() != null ? shipment.getWarehouse().getId() : null,
                shipment.getSalesOrder() != null ? shipment.getSalesOrder().getId() : null,
                shipment.getSalesOrder() != null ? shipment.getSalesOrder().getOrderNumber() : null,
                shipment.getPickingOrder() != null ? shipment.getPickingOrder().getId() : null,
                shipment.getCustomer() != null ? shipment.getCustomer().getId() : null,
                shipment.getShipmentNumber(),
                shipment.getCarrierName(),
                shipment.getCarrier() != null ? shipment.getCarrier().getId() : null,
                shipment.getFreightModeLabel(),
                shipment.getFreightMode() != null ? shipment.getFreightMode().getId() : null,
                shipment.getFreightAmount(),
                shipment.getTrackingCode(),
                shipment.getPackageCount() != null ? shipment.getPackageCount() : 0,
                shipment.getTotalWeight(),
                shipment.getStatus(),
                shipment.getExpectedDelivery(),
                shipment.getAddressSnapshot(),
                shipment.getResponsibleUser() != null ? shipment.getResponsibleUser().getId() : null,
                shipment.getNotes(),
                shipment.getItems().stream().map(this::toItemResponse).toList(),
                shipment.getPackages().stream().map(this::toPackageResponse).toList(),
                shipment.getTrackingEvents().stream().map(this::toTrackingResponse).toList(),
                shipment.getDeliveryProofs().stream().map(this::toProofResponse).toList(),
                shipment.getVersion(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt());
    }

    public ShipmentItemResponse toItemResponse(ShipmentItem item) {
        return new ShipmentItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getSalesOrderItem() != null ? item.getSalesOrderItem().getId() : null,
                item.getPickingItem() != null ? item.getPickingItem().getId() : null,
                item.getLineNumber(),
                item.getQuantity());
    }

    public ShipmentPackageResponse toPackageResponse(ShipmentPackage pkg) {
        return new ShipmentPackageResponse(
                pkg.getId(),
                pkg.getPackageNumber(),
                pkg.getWeight(),
                pkg.getLengthCm(),
                pkg.getWidthCm(),
                pkg.getHeightCm(),
                pkg.getTrackingCode());
    }

    public ShipmentTrackingResponse toTrackingResponse(ShipmentTracking tracking) {
        return new ShipmentTrackingResponse(
                tracking.getId(),
                tracking.getStatus(),
                tracking.getDescription(),
                tracking.getLocationText(),
                tracking.getOccurredAt());
    }

    public DeliveryProofResponse toProofResponse(DeliveryProof proof) {
        return new DeliveryProofResponse(
                proof.getId(), proof.getProofType(), proof.getStorageRef(), proof.getRecipientName(), proof.getCapturedAt());
    }
}
