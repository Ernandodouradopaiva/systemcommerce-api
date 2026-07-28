package br.com.systemcommerce.finance.receivable.dto;

import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal originalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount,
        ReceivableInstallment.Status status,
        String nossoNumero,
        String billingCode,
        String pixTxid,
        String boletoNumber,
        String notes,
        Long version) {}
