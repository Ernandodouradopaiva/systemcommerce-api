package br.com.systemcommerce.finance.payable.dto;

import br.com.systemcommerce.finance.payable.entity.Payable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        UUID organizationId,
        UUID storeId,
        UUID supplierId,
        String supplierName,
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
        BigDecimal paidAmount,
        BigDecimal balanceAmount,
        Payable.Status status,
        String notes,
        List<PayableInstallmentResponse> installments,
        List<PayableOriginResponse> origins,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
