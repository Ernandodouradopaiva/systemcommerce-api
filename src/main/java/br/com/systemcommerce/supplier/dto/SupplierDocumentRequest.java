package br.com.systemcommerce.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierDocumentRequest(
        @NotBlank(message = "nome do documento é obrigatório") @Size(max = 200) String name,
        @Size(max = 60) String type,
        @Size(max = 500) String fileRef) {}
