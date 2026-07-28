package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PosSettingUpsertRequest(
        @NotBlank @Size(max = 80) String settingKey,
        @NotNull PosSettingScope scope,
        UUID storeId,
        UUID terminalId,
        @NotBlank String value,
        @Size(max = 500) String reason,
        Long expectedVersion) {}
