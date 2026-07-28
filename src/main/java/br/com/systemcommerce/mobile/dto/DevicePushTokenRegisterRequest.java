package br.com.systemcommerce.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DevicePushTokenRegisterRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 20) String platform,
        @NotBlank @Size(max = 500) String token,
        @Size(max = 160) String deviceName,
        @Size(max = 40) String appVersion) {}
