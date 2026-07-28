package br.com.systemcommerce.finance.receivable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableSettlementResponse(
        UUID id,
        UUID organizationId,
        UUID customerId,
        UUID holderId,
        UUID cashSessionId,
        LocalDate paymentDate,
        LocalDate effectiveDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount,
        BigDecimal feeAmount,
        BigDecimal grossAmount,
        BigDecimal acquirerFeeAmount,
        BigDecimal netAmount,
        String status,
        String idempotencyKey,
        UUID holderMovementId,
        Long version) {}
