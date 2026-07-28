package br.com.systemcommerce.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReportRow(
        UUID id,
        String method,
        BigDecimal amount,
        String status,
        Instant paidAt,
        String externalReference,
        String saleNumber) {}
