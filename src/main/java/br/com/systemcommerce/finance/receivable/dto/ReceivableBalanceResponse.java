package br.com.systemcommerce.finance.receivable.dto;

import br.com.systemcommerce.finance.receivable.entity.Receivable;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceivableBalanceResponse(
        UUID receivableId,
        BigDecimal totalAmount,
        BigDecimal receivedAmount,
        BigDecimal balanceAmount,
        Receivable.Status status) {}
