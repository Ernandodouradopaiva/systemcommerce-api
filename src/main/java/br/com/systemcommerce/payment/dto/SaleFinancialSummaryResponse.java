package br.com.systemcommerce.payment.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleFinancialSummaryResponse(
        UUID saleId,
        String saleNumber,
        Sale.SaleStatus saleStatus,
        BigDecimal saleTotal,
        BigDecimal confirmedPaid,
        BigDecimal pendingAmount,
        BigDecimal cancelledAmount,
        BigDecimal refundedAmount,
        BigDecimal balanceDue,
        boolean fullyPaid,
        List<PaymentResponse> payments) {}
