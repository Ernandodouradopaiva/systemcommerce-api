package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PosSettingValidateRequest(
        @NotBlank String settingKey,
        @NotNull PosSettingScope scope,
        UUID storeId,
        UUID terminalId,
        @NotBlank String value) {}
