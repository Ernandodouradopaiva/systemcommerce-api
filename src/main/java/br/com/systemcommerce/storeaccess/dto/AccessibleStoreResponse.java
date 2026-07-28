package br.com.systemcommerce.storeaccess.dto;

import java.util.UUID;

public record AccessibleStoreResponse(
        UUID storeId,
        String storeCode,
        String storeName,
        boolean defaultStore,
        UUID organizationId,
        boolean active,
        boolean allowsSales,
        boolean allowsPos) {}
