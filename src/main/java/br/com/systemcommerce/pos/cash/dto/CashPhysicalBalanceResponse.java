package br.com.systemcommerce.pos.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CashPhysicalBalanceResponse(
        UUID sessionId,
        BigDecimal expectedPhysicalCash,
        BigDecimal opening,
        BigDecimal supplies,
        BigDecimal withdrawals,
        BigDecimal cashSales,
        BigDecimal cashRefunds,
        BigDecimal adjustmentsNet) {}
