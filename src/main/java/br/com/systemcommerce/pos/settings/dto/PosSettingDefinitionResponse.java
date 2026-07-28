package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.entity.PosSettingValueType;
import java.math.BigDecimal;
import java.util.UUID;

public record PosSettingDefinitionResponse(
        UUID id,
        String settingKey,
        PosSettingValueType valueType,
        String category,
        String label,
        String description,
        String defaultValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        String allowedValues,
        boolean critical,
        int sortOrder) {}
