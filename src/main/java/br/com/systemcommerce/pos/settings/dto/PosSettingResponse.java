package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.entity.PosSettingValueType;
import java.time.Instant;
import java.util.UUID;

public record PosSettingResponse(
        UUID id,
        String settingKey,
        PosSettingValueType valueType,
        String category,
        String label,
        boolean critical,
        PosSettingScope scope,
        UUID storeId,
        String storeCode,
        UUID terminalId,
        String terminalCode,
        String value,
        Instant updatedAt,
        Long version) {}
