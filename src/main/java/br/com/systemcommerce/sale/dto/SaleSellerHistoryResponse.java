package br.com.systemcommerce.sale.dto;

import java.time.Instant;
import java.util.UUID;

public record SaleSellerHistoryResponse(
        UUID id,
        UUID previousSellerProfileId,
        UUID newSellerProfileId,
        String previousSellerCode,
        String newSellerCode,
        String previousSellerName,
        String newSellerName,
        UUID changedById,
        String changedByName,
        String reason,
        Instant createdAt) {}
