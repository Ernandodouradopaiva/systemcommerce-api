package br.com.systemcommerce.pos.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StorageLocationRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 80) String code,
        @Size(max = 80) String barcode,
        UUID zoneId,
        UUID aisleId,
        UUID rackId,
        UUID shelfId,
        Boolean trackBalance) {}
