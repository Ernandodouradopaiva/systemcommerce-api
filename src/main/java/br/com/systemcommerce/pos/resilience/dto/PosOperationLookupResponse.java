package br.com.systemcommerce.pos.resilience.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultado da consulta de operação PDV por Idempotency-Key (resiliência / resposta perdida).
 * Não inclui dados sensíveis de cartão.
 */
public record PosOperationLookupResponse(
        String idempotencyKey,
        boolean found,
        String operationType,
        String outcome,
        UUID saleId,
        String saleNumber,
        Sale.SaleStatus saleStatus,
        Long saleVersion,
        UUID paymentId,
        Payment.PaymentStatus paymentStatus,
        Payment.PaymentMethod paymentMethod,
        BigDecimal paymentAmount,
        UUID entityId,
        String entityName,
        String message) {

    public static PosOperationLookupResponse notFound(String key) {
        return new PosOperationLookupResponse(
                key,
                false,
                null,
                "NOT_FOUND",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Nenhuma operação encontrada para esta Idempotency-Key");
    }
}
