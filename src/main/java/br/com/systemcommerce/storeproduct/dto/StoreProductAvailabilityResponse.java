package br.com.systemcommerce.storeproduct.dto;

import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import java.util.UUID;

public record StoreProductAvailabilityResponse(
        UUID productId,
        UUID storeId,
        SaleChannel channel,
        boolean sellable,
        String reason,
        StoreProduct.StoreProductStatus status) {}
