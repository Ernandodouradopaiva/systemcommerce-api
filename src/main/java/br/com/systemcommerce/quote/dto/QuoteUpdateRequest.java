package br.com.systemcommerce.quote.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteUpdateRequest(
        UUID customerId,
        UUID sellerId,
        UUID sellerProfileId,
        UUID priceTableId,
        @Size(max = 30) String channel,
        @Size(max = 200) String paymentCondition,
        @Size(max = 200) String carrierName,
        LocalDate expectedDeliveryDate,
        @Min(1) @Max(3650) Integer validityDays,
        LocalDate validUntil,
        @Size(max = 2000) String notes,
        Boolean reserveStock,
        @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo") BigDecimal discountAmount,
        @DecimalMin(value = "0.00", message = "Frete não pode ser negativo") BigDecimal freightAmount,
        @DecimalMin(value = "0.00", message = "Acréscimo não pode ser negativo") BigDecimal surchargeAmount,
        @NotEmpty @Valid List<QuoteItemRequest> items,
        String changeNotes) {}
