package br.com.systemcommerce.storeaccess.dto;

import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UserStoreAccessGrantRequest(
        @NotNull UUID userId,
        @NotNull UUID storeId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Boolean defaultStore,
        UserStoreAccess.AccessType accessType,
        @Size(max = 500) String reason) {}
