package br.com.systemcommerce.customerstore.dto;

import java.util.UUID;

public record CustomerOriginStoreResponse(
        UUID customerId, UUID originStoreId, String originStoreCode, String originStoreName) {}
