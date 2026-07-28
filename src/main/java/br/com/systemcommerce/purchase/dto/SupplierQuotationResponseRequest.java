package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SupplierQuotationResponseRequest(
        @Size(max = 200) String paymentCondition,
        @DecimalMin(value = "0.00") BigDecimal freightAmount,
        @DecimalMin(value = "0.00") BigDecimal taxAmount,
        @DecimalMin(value = "0.00") BigDecimal discountAmount,
        Integer leadTimeDays,
        LocalDate validUntil,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<SupplierQuotationResponseItemRequest> items) {}
