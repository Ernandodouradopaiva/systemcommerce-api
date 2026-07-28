package br.com.systemcommerce.shared.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String module,
        String entityName,
        UUID entityId,
        String action,
        Object oldValues,
        Object newValues,
        String details,
        String ipAddress,
        String correlationId,
        UUID storeId,
        UUID organizationId,
        UUID performedById,
        String performedByName,
        String performedByLogin,
        Instant performedAt) {}
