package br.com.systemcommerce.pos.cancellation.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CancellationRefundResponse(
        UUID id,
        UUID paymentId,
        CancellationRefund.Status status,
        Payment.PaymentMethod method,
        BigDecimal amount,
        String failureReason,
        Integer attempts,
        Instant lastAttemptAt,
        Instant completedAt) {}
