package br.com.systemcommerce.fiscal.security.dto;

import java.util.Map;
import java.util.UUID;

public record FiscalAuditEventRequest(
        UUID organizationId,
        UUID storeId,
        UUID establishmentId,
        UUID userId,
        UUID documentId,
        String action,
        String entityType,
        UUID entityId,
        String ipAddress,
        String correlationId,
        String result,
        String resultCode,
        Map<String, Object> before,
        Map<String, Object> after,
        String details) {}
