package br.com.systemcommerce.bundle.dto;

import java.math.BigDecimal;

public record BundleAvailabilityResponse(BigDecimal availableBundles, boolean sufficient) {}
