package br.com.systemcommerce.seller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SellerStoreAuthorizeRequest(
        @NotNull UUID storeId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Boolean primary,
        Boolean temporary,
        Boolean allowsRegisterSale,
        @DecimalMin("0") @DecimalMax("100") BigDecimal maxDiscountPercent,
        BigDecimal targetAmount,
        @Size(max = 2000) String notes) {}
