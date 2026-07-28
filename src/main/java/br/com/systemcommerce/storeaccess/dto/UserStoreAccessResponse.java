package br.com.systemcommerce.storeaccess.dto;

import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserStoreAccessResponse(
        UUID id,
        UUID userId,
        String userLogin,
        UUID storeId,
        String storeCode,
        String storeName,
        LocalDate startDate,
        LocalDate endDate,
        boolean defaultStore,
        UserStoreAccess.AccessType accessType,
        UserStoreAccess.AccessStatus status,
        UUID grantedById,
        String reason,
        Instant createdAt,
        Instant updatedAt) {}
