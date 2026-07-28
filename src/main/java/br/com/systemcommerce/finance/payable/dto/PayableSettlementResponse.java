package br.com.systemcommerce.finance.payable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableSettlementResponse(
        UUID id,
        UUID organizationId,
        UUID holderId,
        LocalDate paymentDate,
        LocalDate effectiveDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount,
        BigDecimal feeAmount,
        BigDecimal totalDisbursed,
        String status,
        String idempotencyKey,
        UUID holderMovementId,
        Long version) {}
