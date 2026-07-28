package br.com.systemcommerce.finance.card.dto;

import br.com.systemcommerce.finance.card.entity.CardFeePlan;
import br.com.systemcommerce.finance.card.entity.CardTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CardDtos {
    private CardDtos() {}

    public record AcquirerCreateRequest(@NotNull UUID organizationId, @NotBlank String code, @NotBlank String name, String document) {}
    public record BrandCreateRequest(@NotNull UUID organizationId, @NotBlank String code, @NotBlank String name) {}
    public record FeePlanCreateRequest(
            @NotNull UUID organizationId,
            @NotNull UUID acquirerId,
            UUID cardBrandId,
            @NotBlank String code,
            @NotBlank String name,
            @NotNull CardFeePlan.Modality modality,
            Integer installmentFrom,
            Integer installmentTo,
            BigDecimal feePercent,
            BigDecimal feeFixed,
            Integer settlementDays,
            @NotNull LocalDate validFrom,
            LocalDate validTo) {}

    public record RegisterTransactionRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            UUID saleId,
            UUID paymentId,
            @NotNull UUID acquirerId,
            UUID cardBrandId,
            UUID feePlanId,
            @NotNull CardTransaction.Modality modality,
            @NotNull Integer installments,
            @NotNull @DecimalMin("0.01") BigDecimal grossAmount,
            String nsu,
            String authorizationCode,
            String cardLastFour,
            UUID cashSessionId,
            UUID terminalId,
            @NotBlank String idempotencyKey) {}

    public record SettleRequest(
            @NotNull UUID organizationId,
            @NotNull UUID acquirerId,
            @NotNull UUID holderId,
            @NotNull LocalDate settlementDate,
            @NotNull List<UUID> scheduleIds,
            UUID bankStatementEntryId,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record ChargebackRequest(
            @NotNull UUID organizationId,
            UUID scheduleId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String reason,
            @NotNull LocalDate chargebackDate,
            @NotBlank String idempotencyKey) {}

    public record ScheduleForecastResponse(
            UUID id, UUID transactionId, Integer installmentNumber, LocalDate expectedDate,
            BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount, String status) {}
}
