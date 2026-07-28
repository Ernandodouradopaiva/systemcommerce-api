package br.com.systemcommerce.customerstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CustomerStoreRelationshipCreateRequest(
        @NotNull UUID storeId,
        UUID preferredSellerProfileId,
        @Size(max = 2000) String localNotes) {}
