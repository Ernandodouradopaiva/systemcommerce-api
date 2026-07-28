package br.com.systemcommerce.bundle.dto;

import br.com.systemcommerce.bundle.entity.BundleInventoryPolicyType;
import br.com.systemcommerce.bundle.entity.BundlePricePolicyType;
import br.com.systemcommerce.bundle.entity.ProductBundleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductBundleCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID productId,
        @NotNull ProductBundleType bundleType,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        BundlePricePolicyType pricePolicy,
        BundleInventoryPolicyType inventoryPolicy,
        BigDecimal fixedPrice,
        BigDecimal componentDiscountPct,
        @NotEmpty @Valid List<BundleItemRequest> items) {

    public record BundleItemRequest(
            @NotNull UUID componentProductId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotNull Integer lineNumber,
            Boolean optionalComponent) {}
}
