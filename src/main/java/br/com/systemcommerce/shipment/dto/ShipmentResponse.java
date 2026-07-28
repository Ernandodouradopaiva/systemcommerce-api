package br.com.systemcommerce.shipment.dto;

import br.com.systemcommerce.shipment.entity.Shipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID warehouseId,
        UUID salesOrderId,
        String salesOrderNumber,
        UUID pickingOrderId,
        UUID customerId,
        String shipmentNumber,
        String carrierName,
        UUID carrierId,
        String freightModeLabel,
        UUID freightModeId,
        BigDecimal freightAmount,
        String trackingCode,
        int packageCount,
        BigDecimal totalWeight,
        Shipment.ShipmentStatus status,
        LocalDate expectedDelivery,
        String addressSnapshot,
        UUID responsibleUserId,
        String notes,
        List<ShipmentItemResponse> items,
        List<ShipmentPackageResponse> packages,
        List<ShipmentTrackingResponse> trackingEvents,
        List<DeliveryProofResponse> deliveryProofs,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
