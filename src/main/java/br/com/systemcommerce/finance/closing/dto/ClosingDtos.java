package br.com.systemcommerce.finance.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ClosingDtos {
    private ClosingDtos() {}

    public record PeriodCreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotBlank String code,
            @NotBlank String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String timezone,
            String notes) {}

    public record CloseRequest(String notes, Boolean forceWarnings) {}

    public record ReopenRequest(@NotBlank String reason, UUID approvalRequestId) {}

    public record PeriodResponse(
            UUID id,
            UUID organizationId,
            UUID storeId,
            String code,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String timezone,
            String status,
            String notes) {}

    public record CheckResponse(String checkCode, String severity, boolean passed, String message, String details) {}

    public record SnapshotResponse(UUID holderId, String holderCode, String holderName, BigDecimal balanceAmount) {}

    public record ClosingResponse(
            UUID id,
            UUID periodId,
            Instant closedAt,
            UUID closedBy,
            String notes,
            int blockersCount,
            int warningsCount,
            List<CheckResponse> checks,
            List<SnapshotResponse> balanceSnapshots) {}
}
