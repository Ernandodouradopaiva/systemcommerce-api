package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.pos.cash.entity.CashMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementResponse(
        UUID id,
        UUID cashSessionId,
        CashMovement.MovementType type,
        BigDecimal amount,
        Instant occurredAt,
        String description,
        String reason,
        UUID reasonId,
        String reasonCode,
        String reasonDescription,
        String notes,
        UUID executedById,
        String executedByName,
        UUID authorizedById,
        String authorizedByName,
        UUID saleId,
        String originType,
        UUID originId,
        UUID reversesMovementId,
        CashMovement.CashEffect cashEffect,
        Boolean affectsPhysicalCash,
        Instant createdAt) {}
