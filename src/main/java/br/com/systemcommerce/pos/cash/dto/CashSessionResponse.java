package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.pos.cash.entity.CashSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashSessionResponse(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID terminalId,
        String terminalCode,
        Integer terminalNumber,
        UUID operatorId,
        String operatorName,
        Instant openedAt,
        Instant closedAt,
        BigDecimal openingAmount,
        CashSession.CashSessionStatus status,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal differenceAmount,
        String openingNotes,
        String closingNotes,
        UUID authorizedById,
        String authorizedByName,
        Boolean canStartClosing,
        Boolean canClose,
        Boolean canCancel,
        Boolean canRegisterMovement,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
