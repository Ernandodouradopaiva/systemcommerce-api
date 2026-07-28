package br.com.systemcommerce.bi.dto;

import java.time.Instant;
import java.util.UUID;

public record BiRefreshLogResponse(
        UUID id,
        String objectName,
        String refreshType,
        Instant startedAt,
        Instant finishedAt,
        String status,
        Long rowsAffected,
        String errorMessage) {}
