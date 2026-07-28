package br.com.systemcommerce.mobile.dto;

import java.time.Instant;
import java.util.UUID;

public record DevicePushTokenResponse(
        UUID id, UUID userId, String platform, String deviceName, String appVersion, Instant lastSeenAt) {}
