package br.com.systemcommerce.fiscal.migration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Importação formal de DFe já emitido fora do SystemCommerce.
 * Não gera estoque nem financeiro e não emite documento novo na SEFAZ.
 */
public record ExternalFiscalHistoryImportRequest(
        @NotNull UUID organizationId,
        @NotNull UUID storeId,
        @NotNull UUID establishmentId,
        @NotBlank String model,
        @NotBlank String series,
        @NotNull Long number,
        @NotBlank String accessKey,
        @NotBlank String xmlContent,
        @NotBlank String protocolNumber,
        Instant issueDateTime,
        String status,
        String environment,
        String sourceSystem,
        UUID migrationBatchId,
        String originDocumentType,
        UUID originDocumentId,
        @NotBlank String idempotencyKey,
        String formalProcedureReference) {}
