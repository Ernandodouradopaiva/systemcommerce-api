package br.com.systemcommerce.production.dto;

import br.com.systemcommerce.production.entity.ProductionOrderStatus;
import java.time.Instant;

public record ProductionOrderStatusHistoryResponse(
        ProductionOrderStatus fromStatus, ProductionOrderStatus toStatus, String notes, Instant changedAt) {}
