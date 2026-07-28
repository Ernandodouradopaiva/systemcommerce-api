package br.com.systemcommerce.pos.cancellation.dto;

import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.sale.entity.Sale;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleCancellationResponse(
        UUID id,
        UUID saleId,
        String saleNumber,
        Sale.SaleStatus saleStatus,
        SaleCancellation.Status status,
        String reason,
        UUID requestedById,
        String requestedByName,
        UUID authorizedById,
        String authorizedByName,
        UUID executedById,
        String executedByName,
        Instant requestedAt,
        Instant authorizedAt,
        Instant executedAt,
        String decisionNotes,
        String failureDetail,
        List<CancellationRefundResponse> refunds,
        Long version) {}
