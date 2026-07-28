package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.pos.cash.entity.CashMovementReason;
import java.util.UUID;

public record CashMovementReasonResponse(
        UUID id, String code, String description, CashMovementReason.AppliesTo appliesTo, Boolean active) {}
