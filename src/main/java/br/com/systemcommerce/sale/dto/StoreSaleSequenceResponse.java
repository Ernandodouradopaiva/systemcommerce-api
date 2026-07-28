package br.com.systemcommerce.sale.dto;

import java.util.UUID;

public record StoreSaleSequenceResponse(
        UUID storeId,
        String storeCode,
        String prefix,
        long lastValue,
        String nextSaleNumberPreview) {}
