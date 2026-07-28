package br.com.systemcommerce.pos.settings.dto;

import java.util.List;

public record PosSettingValidateResponse(
        boolean valid,
        String settingKey,
        String normalizedValue,
        List<String> errors) {}
