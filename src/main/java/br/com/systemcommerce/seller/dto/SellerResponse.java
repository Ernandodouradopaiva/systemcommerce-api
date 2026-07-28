package br.com.systemcommerce.seller.dto;

import br.com.systemcommerce.seller.entity.SellerProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SellerResponse(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        String employeeName,
        String employeeRegistration,
        String sellerCode,
        SellerProfile.SellerStatus status,
        BigDecimal maxDiscountPercent,
        boolean allowsExternalSale,
        boolean allowsOtherStores,
        BigDecimal monthlyTargetAmount,
        BigDecimal defaultCommissionPercent,
        UUID supervisorEmployeeId,
        LocalDate enabledAt,
        LocalDate disabledAt,
        String notes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
