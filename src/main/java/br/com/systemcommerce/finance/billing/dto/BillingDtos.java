package br.com.systemcommerce.finance.billing.dto;

import br.com.systemcommerce.finance.billing.entity.BillingDocument;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BillingDtos {
    private BillingDtos() {}

    public record CreateBillingRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull UUID customerId,
            UUID receivableId,
            UUID receivableInstallmentId,
            @NotNull BillingDocument.BillingType billingType,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate dueDate,
            String providerCode,
            /** PIX: segundos até expiração (padrão 3600). */
            Integer pixExpiresInSeconds,
            String notes,
            @NotBlank String idempotencyKey,
            /** Se true, registra imediatamente no adapter. */
            Boolean registerImmediately) {}

    public record RegisterRequest(String providerCode) {}

    public record CancelRequest(String reason) {}

    public record WebhookRequest(
            @NotNull UUID organizationId,
            @NotBlank String providerCode,
            @NotBlank String eventId,
            String eventType,
            String externalId,
            BigDecimal paidAmount,
            Instant paidAt,
            String endToEndId,
            @NotBlank String payload) {}

    public record BillingResponse(
            UUID id,
            UUID organizationId,
            UUID customerId,
            UUID receivableInstallmentId,
            String billingType,
            BigDecimal amount,
            LocalDate dueDate,
            String status,
            String externalId,
            String providerCode,
            BankSlipResponse bankSlip,
            PixChargeResponse pixCharge,
            List<StatusHistoryResponse> statusHistory) {}

    public record BankSlipResponse(
            String digitableLine, String barcode, String nossoNumero, String bankCode, String wallet, Instant registeredAt) {}

    public record PixChargeResponse(
            String txid, String qrCode, String copyPaste, Instant expiresAt, Instant paidAt) {}

    public record StatusHistoryResponse(
            String fromStatus, String toStatus, Instant changedAt, String notes, String externalEventId) {}
}
