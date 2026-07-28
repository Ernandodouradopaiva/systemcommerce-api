package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaleSellerRequest(
        @NotNull UUID sellerProfileId, @Size(max = 500) String reason) {}
