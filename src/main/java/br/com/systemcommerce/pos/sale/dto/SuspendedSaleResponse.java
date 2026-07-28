package br.com.systemcommerce.pos.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resumo oficial de venda suspensa para listagem/recuperação. */
public record SuspendedSaleResponse(
        UUID id,
        String saleNumber,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID terminalId,
        String terminalCode,
        Integer terminalNumber,
        UUID operatorId,
        String operatorName,
        UUID suspendedById,
        String suspendedByName,
        UUID customerId,
        String customerName,
        BigDecimal totalAmount,
        int itemCount,
        Instant suspendedAt,
        Instant suspendExpiresAt,
        boolean expired,
        Long remainingSeconds,
        String suspendReason,
        UUID editLockOwnerId,
        String editLockOwnerName,
        UUID editLockTerminalId,
        String editLockTerminalCode,
        Instant editLockAt,
        boolean locked,
        boolean lockedByOther,
        Long version) {}
