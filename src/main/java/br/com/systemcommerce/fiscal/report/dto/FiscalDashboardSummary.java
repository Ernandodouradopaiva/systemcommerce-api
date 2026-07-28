package br.com.systemcommerce.fiscal.report.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record FiscalDashboardSummary(
        long nfeCount,
        long nfceCount,
        BigDecimal authorizedAmount,
        long rejectedCount,
        List<Map<String, Object>> topRejections,
        long cancelledCount,
        long contingencyCount,
        long pendingEvents,
        long numberingGapsEstimate,
        long inutilizationsCount,
        long incomingCount,
        long pendingManifestations,
        Map<String, Long> byStore,
        Map<String, Long> byUf,
        double avgAuthorizationMs,
        double serviceAvailabilityPct) {}
