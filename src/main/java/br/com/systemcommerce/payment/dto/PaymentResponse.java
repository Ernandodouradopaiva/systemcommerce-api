package br.com.systemcommerce.payment.dto;

import br.com.systemcommerce.payment.entity.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID saleId,
        String saleNumber,
        UUID cashSessionId,
        Payment.PaymentMethod method,
        BigDecimal amount,
        BigDecimal informedAmount,
        BigDecimal appliedAmount,
        BigDecimal changeAmount,
        Payment.PaymentStatus status,
        Instant paidAt,
        String externalReference,
        String notes,
        Integer installments,
        BigDecimal tenderedAmount,
        String authorizationCode,
        String nsu,
        String cardBrand,
        String acquirer,
        String idempotencyKey,
        UUID responsibleUserId,
        String responsibleUserName,
        Instant createdAt,
        Instant updatedAt) {}
