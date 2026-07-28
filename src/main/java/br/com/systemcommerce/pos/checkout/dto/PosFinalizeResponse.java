package br.com.systemcommerce.pos.checkout.dto;

import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.pos.receipt.dto.PosReceiptResponse;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.util.List;

public record PosFinalizeResponse(
        SaleResponse sale,
        List<PaymentResponse> payments,
        BigDecimal total,
        BigDecimal balanceDue,
        BigDecimal changeTotal,
        Sale.SaleStatus status,
        String receiptNumber,
        PosReceiptResponse printData) {}
