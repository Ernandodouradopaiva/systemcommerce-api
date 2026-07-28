package br.com.systemcommerce.shipment.dto;

import java.time.Instant;
import java.util.UUID;

public record ShipmentTrackingResponse(
        UUID id, String status, String description, String locationText, Instant occurredAt) {}
