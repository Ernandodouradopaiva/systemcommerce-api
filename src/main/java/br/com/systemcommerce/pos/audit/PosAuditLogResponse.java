package br.com.systemcommerce.pos.audit;

import java.time.Instant;
import java.util.UUID;

public record PosAuditLogResponse(
        UUID id,
        String eventCode,
        String outcome,
        String errorCode,
        String module,
        String entityName,
        UUID entityId,
        String action,
        UUID storeId,
        UUID terminalId,
        UUID cashSessionId,
        UUID saleId,
        UUID operatorId,
        String operatorName,
        UUID authorizedById,
        String authorizedByName,
        UUID performedById,
        String performedByName,
        Object before,
        Object after,
        String details,
        String ipAddress,
        String correlationId,
        Instant performedAt) {}
