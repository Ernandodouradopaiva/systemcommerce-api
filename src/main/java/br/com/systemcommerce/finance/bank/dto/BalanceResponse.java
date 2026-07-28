package br.com.systemcommerce.finance.bank.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(UUID holderId, BigDecimal balance, Instant computedAt) {}
