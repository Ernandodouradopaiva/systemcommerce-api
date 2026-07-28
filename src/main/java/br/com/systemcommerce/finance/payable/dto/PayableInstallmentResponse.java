package br.com.systemcommerce.finance.payable.dto;

import br.com.systemcommerce.finance.payable.entity.PayableInstallment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal originalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount,
        BigDecimal settledAmount,
        BigDecimal balanceAmount,
        PayableInstallment.Status status,
        String barcode,
        String digitableLine,
        String referenceCode,
        Long version) {}
