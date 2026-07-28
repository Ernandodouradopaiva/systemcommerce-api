package br.com.systemcommerce.supplier.dto;

import java.time.Instant;
import java.util.UUID;

public record SupplierDocumentResponse(
        UUID id,
        UUID supplierId,
        String name,
        String type,
        String fileRef,
        Instant uploadedAt,
        Boolean active) {}
