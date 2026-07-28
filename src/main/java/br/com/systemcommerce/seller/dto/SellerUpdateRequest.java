package br.com.systemcommerce.seller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SellerUpdateRequest(
        @NotBlank @Size(max = 40) String sellerCode,
        @DecimalMin("0") @DecimalMax("100") BigDecimal maxDiscountPercent,
        Boolean allowsExternalSale,
        Boolean allowsOtherStores,
        BigDecimal monthlyTargetAmount,
        @DecimalMin("0") @DecimalMax("100") BigDecimal defaultCommissionPercent,
        UUID supervisorEmployeeId,
        @Size(max = 2000) String notes) {}
