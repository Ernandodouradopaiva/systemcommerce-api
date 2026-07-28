package br.com.systemcommerce.fiscal.migration.controller;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.migration.dto.ExternalFiscalHistoryImportRequest;
import br.com.systemcommerce.fiscal.migration.service.ExternalFiscalHistoryService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/migration")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Migration", description = "Migração e histórico externo (Prompt 150)")
public class FiscalMigrationController {

    private final ExternalFiscalHistoryService externalFiscalHistoryService;

    @PostMapping("/external-history")
    @PreAuthorize("hasAuthority('FISCAL_HISTORY_IMPORT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExternal(
            @Valid @RequestBody ExternalFiscalHistoryImportRequest request) {
        FiscalDocument doc = externalFiscalHistoryService.importExternalHistory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(Map.of(
                        "id", doc.getId(),
                        "accessKey", doc.getAccessKey(),
                        "status", doc.getStatus().name(),
                        "externalImport", true,
                        "migrationBatchId", doc.getMigrationBatchId(),
                        "message", "Histórico importado sem emissão SEFAZ, estoque ou financeiro")));
    }

    @PostMapping("/documents/{id}/pending-integration")
    @PreAuthorize("hasAuthority('FISCAL_MIGRATION_MANAGE') or hasAuthority('FISCAL_DOCUMENT_TRANSMIT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> pendingIntegration(@PathVariable UUID id) {
        FiscalDocument doc = externalFiscalHistoryService.markAuthorizedPendingIntegration(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of("id", doc.getId().toString(), "status", doc.getStatus().name())));
    }

    @PostMapping("/documents/{id}/complete-integration")
    @PreAuthorize("hasAuthority('FISCAL_MIGRATION_MANAGE') or hasAuthority('FISCAL_DOCUMENT_TRANSMIT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> completeIntegration(@PathVariable UUID id) {
        FiscalDocument doc = externalFiscalHistoryService.markIntegrationComplete(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of("id", doc.getId().toString(), "status", doc.getStatus().name())));
    }
}
