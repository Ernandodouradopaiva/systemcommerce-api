package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptItemRequest(
        @NotNull UUID purchaseOrderItemId,
        @NotNull @DecimalMin(value = "0.0000", message = "Quantidade recebida não pode ser negativa")
                BigDecimal quantityReceived,
        @DecimalMin(value = "0.0000", message = "Quantidade rejeitada não pode ser negativa")
                BigDecimal quantityRejected,
        @Size(max = 60) String batchCode,
        LocalDate expiryDate,
        @Size(max = 120) String serialNumber,
        @Size(max = 120) String destinationLocation) {}
