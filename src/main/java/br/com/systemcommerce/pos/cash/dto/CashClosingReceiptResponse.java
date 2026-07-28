package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payload oficial para impressão/apresentação do comprovante de fechamento. */
public record CashClosingReceiptResponse(
        UUID sessionId,
        String storeCode,
        String storeName,
        String terminalCode,
        Integer terminalNumber,
        String operatorName,
        Instant openedAt,
        Instant closedAt,
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
        String closingNotes,
        List<MethodLine> byPaymentMethod,
        String receiptTitle) {

    public record MethodLine(Payment.PaymentMethod method, BigDecimal expectedAmount, BigDecimal amount) {}
}
