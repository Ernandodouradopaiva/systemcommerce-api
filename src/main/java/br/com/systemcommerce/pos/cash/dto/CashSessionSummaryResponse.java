package br.com.systemcommerce.pos.cash.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CashSessionSummaryResponse(
        UUID sessionId,
        BigDecimal openingAmount,
        BigDecimal supplies,
        BigDecimal withdrawals,
        BigDecimal salesReceived,
        BigDecimal cancellations,
        BigDecimal refunds,
        BigDecimal expectedCash,
        BigDecimal expectedGeneral,
        BigDecimal countedAmount,
        BigDecimal differenceAmount,
        List<PaymentMethodTotal> byPaymentMethod) {}
