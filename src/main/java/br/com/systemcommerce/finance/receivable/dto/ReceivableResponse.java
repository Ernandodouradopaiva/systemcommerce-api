package br.com.systemcommerce.finance.receivable.dto;

import br.com.systemcommerce.finance.receivable.entity.Receivable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID customerId,
        String customerName,
        UUID paymentConditionId,
        UUID financialCategoryId,
        UUID costCenterId,
        String documentNumber,
        LocalDate issueDate,
        LocalDate competenceDate,
        BigDecimal originalAmount,
        BigDecimal plannedDiscount,
        BigDecimal plannedAddition,
        BigDecimal totalAmount,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount,
        Receivable.Status status,
        String notes,
        List<ReceivableInstallmentResponse> installments,
        List<ReceivableOriginResponse> origins,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
