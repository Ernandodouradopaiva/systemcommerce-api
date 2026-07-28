package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GoodsReceiptInspectionRequest(@NotEmpty @Valid List<GoodsReceiptItemInspection> items) {

    public record GoodsReceiptItemInspection(
            java.util.UUID receiptItemId,
            java.math.BigDecimal quantityAccepted,
            String divergenceDescription,
            String divergenceType) {}
}
