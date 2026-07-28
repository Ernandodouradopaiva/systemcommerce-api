package br.com.systemcommerce.settings.dto;

import br.com.systemcommerce.settings.entity.SystemSettingScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SystemSettingUpsertRequest(
        @NotBlank @Size(max = 80) String settingKey,
        @NotNull SystemSettingScope scope,
        UUID organizationId,
        UUID storeGroupId,
        UUID storeId,
        UUID terminalId,
        UUID userId,
        @NotBlank String value,
        Long expectedVersion) {}
