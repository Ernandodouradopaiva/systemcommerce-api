package br.com.systemcommerce.shipment.dto;

import jakarta.validation.constraints.NotBlank;

/** Evento de rastreio; se {@code status} corresponder a um status válido de expedição, o cabeçalho é atualizado. */
public record ShipmentTrackingRequest(@NotBlank String status, String description, String locationText) {}
