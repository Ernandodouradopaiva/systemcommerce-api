package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingHistory;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import java.time.Instant;
import java.util.UUID;

public record PosSettingHistoryResponse(
        UUID id,
        UUID settingId,
        String settingKey,
        PosSettingScope scope,
        UUID storeId,
        UUID terminalId,
        String oldValue,
        String newValue,
        PosSettingHistory.ChangeType changeType,
        String reason,
        UUID changedById,
        String changedByName,
        Instant changedAt,
        String correlationId) {}
