package br.com.systemcommerce.settings.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SystemSettingCopyRequest(
        @NotNull UUID sourceStoreId, @NotNull UUID targetStoreId, UUID organizationId) {}
