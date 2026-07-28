package br.com.systemcommerce.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Agregação por cliente/produto/vendedor. */
public record AggregationReportRow(
        UUID id, String code, String name, long count, BigDecimal quantity, BigDecimal amount) {}
