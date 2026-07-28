package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PriceTableCustomerGroupRequest(
        @NotBlank(message = "código do grupo é obrigatório") @Size(max = 60) String customerGroupCode,
        @Size(max = 120) String customerGroupName) {}
