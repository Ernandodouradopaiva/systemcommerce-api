package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PriceTableGroupLinkRequest(@NotNull(message = "grupo é obrigatório") UUID storeGroupId) {}
