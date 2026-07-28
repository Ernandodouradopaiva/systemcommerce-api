package br.com.systemcommerce.settings.dto;

import br.com.systemcommerce.settings.entity.SystemSettingScope;
import java.time.Instant;
import java.util.UUID;

public record SystemSettingResponse(
        UUID id,
        String settingKey,
        SystemSettingScope scope,
        UUID organizationId,
        UUID storeGroupId,
        UUID storeId,
        UUID terminalId,
        UUID userId,
        String value,
        Boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
