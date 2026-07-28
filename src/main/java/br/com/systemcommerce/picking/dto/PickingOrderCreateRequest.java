package br.com.systemcommerce.picking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PickingOrderCreateRequest(@NotNull UUID salesOrderId, String notes) {}
