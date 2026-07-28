package br.com.systemcommerce.pos.receipt.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload oficial de comprovante PDV. Front apenas formata — não recalcula.
 * Sempre documento não fiscal enquanto não houver integração fiscal válida.
 */
public record PosReceiptResponse(
        ReceiptPrintLog.PrintType type,
        String title,
        boolean nonFiscal,
        String documentDisclaimer,
        String footerMessage,
        String authenticationId,
        Integer sequenceNo,
        Instant issuedAt,
        PosTerminal.PrintModel suggestedLayout,
        String printerName,
        boolean reprint,
        String reprintReason,
        Integer copies,
        UUID printLogId,
        StoreBlock store,
        TerminalBlock terminal,
        OperatorBlock operator,
        CustomerBlock customer,
        SaleBlock sale,
        List<ItemLine> items,
        TotalsBlock totals,
        List<PaymentLine> payments,
        CashMovementBlock cashMovement,
        SessionCloseBlock sessionClose,
        CancellationBlock cancellation,
        PaymentDetailBlock paymentDetail) {

    public record StoreBlock(
            UUID id,
            String code,
            String name,
            String tradeName,
            String document,
            String stateRegistration,
            String phone,
            String email,
            String addressLine,
            String zipCode,
            String city,
            String state) {}

    public record TerminalBlock(
            UUID id, String code, Integer number, String name, PosTerminal.PrintModel printModel, String printerName) {}

    public record OperatorBlock(UUID id, String name, String username) {}

    public record CustomerBlock(UUID id, String name, String document) {}

    public record SaleBlock(
            UUID id,
            String saleNumber,
            String receiptNumber,
            Sale.SaleStatus status,
            Instant saleDate,
            UUID cashSessionId) {}

    public record ItemLine(
            String productName,
            String sku,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal lineTotal) {}

    public record TotalsBlock(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal surchargeTotal,
            BigDecimal total,
            BigDecimal confirmedPaid,
            BigDecimal balanceDue,
            BigDecimal changeTotal) {}

    public record PaymentLine(
            UUID id,
            Payment.PaymentMethod method,
            Payment.PaymentStatus status,
            BigDecimal amount,
            BigDecimal appliedAmount,
            BigDecimal changeAmount,
            String authorizationCode,
            String nsu,
            String cardBrand,
            Instant paidAt) {}

    public record CashMovementBlock(
            UUID id,
            CashMovement.MovementType movementType,
            BigDecimal amount,
            Instant occurredAt,
            String reason,
            String notes,
            String executedByName,
            String authorizedByName,
            BigDecimal expectedPhysicalCashAfter) {}

    public record SessionCloseBlock(
            UUID sessionId,
            CashSession.CashSessionStatus status,
            Instant openedAt,
            Instant closedAt,
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
            List<MethodTotalLine> byPaymentMethod) {}

    public record MethodTotalLine(Payment.PaymentMethod method, BigDecimal amount) {}

    public record CancellationBlock(
            UUID id,
            UUID saleId,
            String saleNumber,
            String status,
            String reason,
            Instant requestedAt,
            Instant executedAt,
            String requestedByName,
            String authorizedByName,
            String executedByName,
            List<RefundLine> refunds) {}

    public record RefundLine(UUID id, String status, BigDecimal amount, Payment.PaymentMethod method) {}

    public record PaymentDetailBlock(
            UUID paymentId,
            Payment.PaymentMethod method,
            Payment.PaymentStatus status,
            BigDecimal amount,
            BigDecimal appliedAmount,
            BigDecimal changeAmount,
            String authorizationCode,
            String nsu,
            Instant paidAt) {}
}
