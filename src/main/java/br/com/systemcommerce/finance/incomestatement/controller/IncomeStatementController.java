package br.com.systemcommerce.finance.incomestatement.controller;

import br.com.systemcommerce.finance.incomestatement.dto.IncomeStatementDtos.*;
import br.com.systemcommerce.finance.incomestatement.service.IncomeStatementService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "DRE gerencial", description = "Demonstração de resultado gerencial — não constitui demonstração contábil oficial (Prompt 116)")
public class IncomeStatementController {

    private final IncomeStatementService service;

    @GetMapping("/income-statement-layouts")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_READ')")
    @Operation(summary = "Listar layouts da DRE gerencial")
    public ResponseEntity<ApiResponse<List<LayoutResponse>>> listLayouts(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.listLayouts(organizationId)));
    }

    @PostMapping("/income-statement-layouts/default")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_MANAGE')")
    @Operation(summary = "Garantir layout padrão da DRE gerencial")
    public ResponseEntity<ApiResponse<LayoutResponse>> ensureDefault(@RequestParam UUID organizationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.ensureDefaultLayout(organizationId)));
    }

    @GetMapping("/income-statement-layouts/{id}")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_READ')")
    @Operation(summary = "Obter layout da DRE gerencial")
    public ResponseEntity<ApiResponse<LayoutResponse>> getLayout(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.getLayout(id)));
    }

    @PostMapping("/income-statement-layouts/{layoutId}/mappings")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_MANAGE')")
    @Operation(summary = "Mapear categoria/conta para linha da DRE gerencial")
    public ResponseEntity<ApiResponse<MappingResponse>> mapCategory(
            @PathVariable UUID layoutId, @Valid @RequestBody MappingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.mapCategory(layoutId, request)));
    }

    @PostMapping("/income-statement/execute")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_READ')")
    @Operation(summary = "Executar DRE gerencial e persistir resultado")
    public ResponseEntity<ApiResponse<ExecutionResponse>> execute(@Valid @RequestBody ExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.execute(request)));
    }

    @GetMapping("/income-statement/executions/{id}")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_READ')")
    @Operation(summary = "Consultar execução da DRE gerencial")
    public ResponseEntity<ApiResponse<ExecutionResponse>> getExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.getExecution(id)));
    }

    @GetMapping("/income-statement/drill-down")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_READ')")
    @Operation(summary = "Detalhamento de linha da DRE gerencial")
    public ResponseEntity<ApiResponse<List<DrillDownItem>>> drillDown(
            @RequestParam UUID executionId,
            @RequestParam String lineCode,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.of(service.drillDown(new DrillDownQuery(executionId, lineCode, limit))));
    }

    @GetMapping(value = "/income-statement/executions/{id}/export.csv", produces = "text/csv")
    @PreAuthorize("hasAuthority('INCOME_STATEMENT_EXPORT')")
    @Operation(summary = "Exportar DRE gerencial em CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID id) {
        byte[] csv = service.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dre-gerencial.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
