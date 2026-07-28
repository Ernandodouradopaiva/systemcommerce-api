package br.com.systemcommerce.payment.dto;

import br.com.systemcommerce.payment.entity.Payment;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusHistoryResponse(
        UUID id,
        Payment.PaymentStatus fromStatus,
        Payment.PaymentStatus toStatus,
        String reason,
        Instant changedAt,
        UUID changedById,
        String changedByName) {}
