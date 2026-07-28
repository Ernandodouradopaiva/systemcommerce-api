package br.com.systemcommerce.picking.dto;

import br.com.systemcommerce.picking.entity.PickingDivergence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PickingDivergenceRequest(
        @NotNull UUID itemId,
        @NotNull PickingDivergence.DivergenceType divergenceType,
        @NotNull @Size(max = 1000) String description,
        BigDecimal quantity) {}
