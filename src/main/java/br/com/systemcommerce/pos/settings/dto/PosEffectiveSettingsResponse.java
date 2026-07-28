package br.com.systemcommerce.pos.settings.dto;

import java.util.List;
import java.util.UUID;

public record PosEffectiveSettingsResponse(
        UUID storeId,
        UUID terminalId,
        List<PosEffectiveSettingItem> settings) {}
