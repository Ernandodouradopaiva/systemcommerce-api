package br.com.systemcommerce.seller.dto;

import br.com.systemcommerce.seller.entity.SellerStoreAssignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SellerStoreAssignmentResponse(
        UUID id,
        UUID sellerProfileId,
        UUID storeId,
        String storeCode,
        String storeName,
        LocalDate startDate,
        LocalDate endDate,
        boolean primaryAssignment,
        boolean temporaryAssignment,
        boolean allowsRegisterSale,
        BigDecimal maxDiscountPercent,
        BigDecimal targetAmount,
        SellerStoreAssignment.AssignmentStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
