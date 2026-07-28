package br.com.systemcommerce.purchase.dto;

import br.com.systemcommerce.purchase.entity.PurchaseQuotationSupplier;
import java.time.Instant;
import java.util.UUID;

public record PurchaseQuotationSupplierResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        Instant invitedAt,
        PurchaseQuotationSupplier.InviteStatus status,
        String notes,
        boolean hasResponse) {}
