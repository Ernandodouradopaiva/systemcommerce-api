package br.com.systemcommerce.payment.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.util.UUID;

public record SalePaymentBalanceResponse(
        UUID saleId,
        String saleNumber,
        Sale.SaleStatus saleStatus,
        BigDecimal saleTotal,
        BigDecimal confirmedPaid,
        BigDecimal balanceDue) {}
