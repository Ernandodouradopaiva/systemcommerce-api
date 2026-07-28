package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Resultado oficial da conferência (pré-fechamento). Front apenas exibe.
 */
public record CashConferenceResponse(
        UUID sessionId,
        CashSession.CashSessionStatus status,
        BigDecimal openingAmount,
        BigDecimal supplies,
        BigDecimal withdrawals,
        BigDecimal salesReceived,
        BigDecimal cancellations,
        BigDecimal refunds,
        long salesCount,
        long cancelledSalesCount,
        BigDecimal expectedCash,
        BigDecimal expectedGeneral,
        BigDecimal countedAmount,
        BigDecimal differenceAmount,
        boolean requiresJustification,
        List<MethodConferenceLine> byPaymentMethod) {

    public record MethodConferenceLine(
            Payment.PaymentMethod method,
            BigDecimal expectedAmount,
            BigDecimal countedAmount,
            BigDecimal differenceAmount) {}
}
