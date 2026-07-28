package br.com.systemcommerce.pos.receipt.dto;

import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import java.time.Instant;
import java.util.UUID;

public record ReceiptPrintLogResponse(
        UUID id,
        ReceiptPrintLog.PrintType printType,
        Integer sequenceNo,
        UUID saleId,
        UUID paymentId,
        UUID cashSessionId,
        UUID cashMovementId,
        UUID saleCancellationId,
        UUID requestedById,
        String requestedByName,
        String reason,
        Integer copies,
        ReceiptPrintLog.PrintLayout layout,
        Boolean isReprint,
        UUID originalLogId,
        String authenticationId,
        UUID terminalId,
        String notes,
        Instant createdAt) {}
