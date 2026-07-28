package br.com.systemcommerce.fiscal.inbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IncomingFiscalImportRequest(
        @NotNull UUID organizationId,
        @NotNull UUID storeId,
        @NotBlank String xml) {}
