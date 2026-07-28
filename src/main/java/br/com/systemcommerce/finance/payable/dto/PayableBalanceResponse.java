package br.com.systemcommerce.finance.payable.dto;

import br.com.systemcommerce.finance.payable.entity.Payable;
import java.math.BigDecimal;
import java.util.UUID;

public record PayableBalanceResponse(
        UUID payableId, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balanceAmount, Payable.Status status) {}
