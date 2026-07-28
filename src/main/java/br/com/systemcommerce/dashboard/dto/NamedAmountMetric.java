package br.com.systemcommerce.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NamedAmountMetric(
        UUID id, String code, String name, BigDecimal quantityOrCount, BigDecimal amount) {}
