package br.com.systemcommerce.bundle.dto;

import br.com.systemcommerce.bundle.entity.BundleInventoryPolicyType;
import br.com.systemcommerce.bundle.entity.BundlePricePolicyType;
import br.com.systemcommerce.bundle.entity.ProductBundleStatus;
import br.com.systemcommerce.bundle.entity.ProductBundleType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductBundleResponse(
        UUID id,
        UUID organizationId,
        UUID productId,
        String productSku,
        ProductBundleType bundleType,
        String code,
        String name,
        String description,
        BundlePricePolicyType pricePolicy,
        BundleInventoryPolicyType inventoryPolicy,
        BigDecimal fixedPrice,
        BigDecimal componentDiscountPct,
        ProductBundleStatus status,
        List<ProductBundleItemResponse> items) {}
