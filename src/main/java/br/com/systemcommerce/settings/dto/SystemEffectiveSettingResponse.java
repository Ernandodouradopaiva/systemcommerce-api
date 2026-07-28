package br.com.systemcommerce.settings.dto;

import br.com.systemcommerce.settings.entity.SystemSettingScope;
import java.util.UUID;

public record SystemEffectiveSettingResponse(
        String settingKey,
        String value,
        SystemSettingScope resolvedFrom,
        UUID settingId,
        String defaultValue) {}
