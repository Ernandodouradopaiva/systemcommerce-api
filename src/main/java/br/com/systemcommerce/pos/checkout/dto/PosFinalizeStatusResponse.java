package br.com.systemcommerce.pos.checkout.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.util.UUID;

public record PosFinalizeStatusResponse(
        UUID saleId,
        String saleNumber,
        Sale.SaleStatus status,
        BigDecimal total,
        BigDecimal confirmedPaid,
        BigDecimal pendingAmount,
        BigDecimal balanceDue,
        long pendingCount,
        long confirmedCount,
        boolean readyToFinalize,
        boolean finalized,
        String message) {}
