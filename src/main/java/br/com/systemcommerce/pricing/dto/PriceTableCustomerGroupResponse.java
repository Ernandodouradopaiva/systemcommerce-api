package br.com.systemcommerce.pricing.dto;

import java.util.UUID;

public record PriceTableCustomerGroupResponse(
        UUID id, UUID priceTableId, String customerGroupCode, String customerGroupName, Boolean active) {}
