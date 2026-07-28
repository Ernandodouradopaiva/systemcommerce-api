package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseReceiptDivergence;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseReceiptDivergenceResponse(
        UUID id,
        UUID purchaseReceiptItemId,
        PurchaseReceiptDivergence.DivergenceType divergenceType,
        String description,
        BigDecimal quantity,
        Instant createdAt) {}
