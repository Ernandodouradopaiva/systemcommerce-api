package br.com.systemcommerce.pos.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PosDashboardResponse(
        Instant generatedAt,
        Instant dayFrom,
        Instant dayToExclusive,
        UUID storeId,
        UUID terminalId,
        MoneyCount salesToday,
        MoneyCount totalReceived,
        long salesInProgress,
        long openCashSessions,
        BigDecimal averageTicket,
        BigDecimal itemsSold,
        long cancellations,
        MoneyCount cashDifferences,
        List<PosPeriodRow> salesByHour,
        List<PosAggRow> paymentsByMethod) {

    public record MoneyCount(BigDecimal amount, long count) {}
}
