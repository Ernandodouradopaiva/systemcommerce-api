package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PriceTableStoreLinkRequest(@NotNull(message = "loja é obrigatória") UUID storeId) {}
