package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CashReconciliationResponse(
        UUID sessionId,
        CashSession.CashSessionStatus status,
        BigDecimal openingAmount,
        BigDecimal supplies,
        BigDecimal withdrawals,
        BigDecimal salesReceived,
        BigDecimal cancellations,
        BigDecimal refunds,
        List<PaymentMethodTotal> byPaymentMethod,
        BigDecimal expectedCash,
        BigDecimal expectedGeneral) {}
