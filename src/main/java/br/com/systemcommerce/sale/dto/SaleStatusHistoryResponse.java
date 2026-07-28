package br.com.systemcommerce.sale.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.time.Instant;
import java.util.UUID;

public record SaleStatusHistoryResponse(
        UUID id,
        Sale.SaleStatus fromStatus,
        Sale.SaleStatus toStatus,
        String reason,
        Instant changedAt,
        UUID changedById,
        String changedByName) {}
