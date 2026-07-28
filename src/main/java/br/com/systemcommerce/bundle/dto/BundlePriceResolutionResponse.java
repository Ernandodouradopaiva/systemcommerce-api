package br.com.systemcommerce.bundle.dto;

import br.com.systemcommerce.bundle.entity.BundlePricePolicyType;
import java.math.BigDecimal;

public record BundlePriceResolutionResponse(BigDecimal resolvedPrice, BundlePricePolicyType pricePolicy) {}
