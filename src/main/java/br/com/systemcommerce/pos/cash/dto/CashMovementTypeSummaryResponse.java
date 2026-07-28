package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.pos.cash.entity.CashMovement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CashMovementTypeSummaryResponse(
        UUID sessionId, List<CashMovementTypeTotal> byType, BigDecimal expectedPhysicalCash) {}
