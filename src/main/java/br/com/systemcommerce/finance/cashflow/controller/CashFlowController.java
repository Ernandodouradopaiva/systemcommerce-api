package br.com.systemcommerce.finance.cashflow.controller;

import br.com.systemcommerce.finance.cashflow.dto.CashFlowDtos.*;
import br.com.systemcommerce.finance.cashflow.service.CashFlowService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@Tag(name = "Fluxo de caixa", description = "Fluxo de caixa realizado e projetado (Prompt 114)")
public class CashFlowController {

    private final CashFlowService service;

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('CASH_FLOW_READ')")
    @Operation(summary = "Consultar fluxo de caixa")
    public ResponseEntity<ApiResponse<CashFlowResponse>> cashFlow(
            @RequestParam UUID organizationId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false, defaultValue = "America/Sao_Paulo") String timezone,
            @RequestParam(required = false) UUID scenarioId,
            @RequestParam(required = false, defaultValue = "CONSOLIDATED") Perspective perspective) {
        var query = new CashFlowQuery(
                organizationId, storeId, holderId, categoryId, costCenterId, from, to, timezone, scenarioId, perspective);
        return ResponseEntity.ok(ApiResponse.of(service.build(query)));
    }

    @GetMapping("/cash-flow/drill-down")
    @PreAuthorize("hasAuthority('CASH_FLOW_READ')")
    @Operation(summary = "Detalhamento de lançamentos do fluxo de caixa")
    public ResponseEntity<ApiResponse<List<DrillDownItem>>> drillDown(
            @RequestParam UUID organizationId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false, defaultValue = "America/Sao_Paulo") String timezone,
            @RequestParam(required = false) UUID scenarioId,
            @RequestParam(required = false, defaultValue = "CONSOLIDATED") Perspective perspective) {
        var query = new CashFlowQuery(
                organizationId, storeId, holderId, categoryId, costCenterId, from, to, timezone, scenarioId, perspective);
        return ResponseEntity.ok(ApiResponse.of(service.drillDown(query, timezone, 200)));
    }

    @GetMapping("/cash-flow-scenarios")
    @PreAuthorize("hasAuthority('CASH_FLOW_READ')")
    @Operation(summary = "Listar cenários de fluxo de caixa")
    public ResponseEntity<ApiResponse<List<ScenarioResponse>>> listScenarios(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.listScenarios(organizationId)));
    }

    @PostMapping("/cash-flow-scenarios")
    @PreAuthorize("hasAuthority('CASH_FLOW_READ')")
    @Operation(summary = "Criar cenário de fluxo de caixa")
    public ResponseEntity<ApiResponse<ScenarioResponse>> createScenario(@Valid @RequestBody ScenarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createScenario(request)));
    }

    @GetMapping(value = "/cash-flow/export.csv", produces = "text/csv")
    @PreAuthorize("hasAuthority('CASH_FLOW_EXPORT')")
    @Operation(summary = "Exportar fluxo de caixa em CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam UUID organizationId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false, defaultValue = "America/Sao_Paulo") String timezone,
            @RequestParam(required = false) UUID scenarioId,
            @RequestParam(required = false, defaultValue = "CONSOLIDATED") Perspective perspective) {
        var query = new CashFlowQuery(
                organizationId, storeId, holderId, categoryId, costCenterId, from, to, timezone, scenarioId, perspective);
        byte[] csv = service.exportCsv(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cash-flow.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
