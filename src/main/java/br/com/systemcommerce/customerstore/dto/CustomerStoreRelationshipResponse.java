package br.com.systemcommerce.customerstore.dto;

import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationshipStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerStoreRelationshipResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerDocument,
        UUID storeId,
        String storeCode,
        String storeName,
        Instant firstServiceAt,
        Instant lastPurchaseAt,
        UUID preferredSellerProfileId,
        String preferredSellerCode,
        String localNotes,
        BigDecimal creditLimitOverride,
        CustomerStoreRelationshipStatus status,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
