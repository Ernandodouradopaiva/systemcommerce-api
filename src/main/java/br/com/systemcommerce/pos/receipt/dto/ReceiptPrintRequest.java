package br.com.systemcommerce.pos.receipt.dto;

import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReceiptPrintRequest(
        @NotNull ReceiptPrintLog.PrintType type,
        UUID saleId,
        UUID paymentId,
        UUID cashSessionId,
        UUID cashMovementId,
        UUID saleCancellationId,
        @NotNull ReceiptPrintLog.PrintLayout layout,
        @NotNull @Min(1) @Max(10) Integer copies,
        @Size(max = 1000) String notes) {}
