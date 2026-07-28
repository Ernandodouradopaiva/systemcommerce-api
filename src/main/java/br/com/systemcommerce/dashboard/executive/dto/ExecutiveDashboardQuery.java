package br.com.systemcommerce.dashboard.executive.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecutiveDashboardQuery(
        ExecutivePerspective perspective,
        UUID organizationId,
        UUID storeGroupId,
        UUID storeId,
        UUID warehouseId,
        String channelCode,
        Instant from,
        Instant to,
        String timezone) {}
