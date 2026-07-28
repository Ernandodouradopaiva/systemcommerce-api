package br.com.systemcommerce.quote.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Métricas simples de conversão de orçamentos (Prompt 64) — contagem por status + taxa de conversão. */
public record QuoteConversionDashboardResponse(
        long totalQuotes,
        long convertedCount,
        long partiallyConvertedCount,
        BigDecimal conversionRatePercent,
        Map<String, Long> countByStatus) {}
