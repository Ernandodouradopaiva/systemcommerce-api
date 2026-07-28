package br.com.systemcommerce.finance.dashboard.controller;

import br.com.systemcommerce.finance.dashboard.dto.FinanceDashboardDtos.*;
import br.com.systemcommerce.finance.dashboard.service.FinanceDashboardService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance-dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Dashboard financeiro", description = "Indicadores financeiros consolidados (Prompt 117)")
public class FinanceDashboardController {

    private static final String DEFAULT_TZ = "America/Sao_Paulo";

    private final FinanceDashboardService service;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_DASHBOARD_READ')")
    @Operation(summary = "Consultar dashboard financeiro")
    public ResponseEntity<ApiResponse<FinanceDashboardResponse>> dashboard(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false, defaultValue = DEFAULT_TZ) String timezone) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneId.of(timezone));
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfMonth(1);
        var query = new FinanceDashboardQuery(
                organizationId,
                storeId,
                storeGroupId,
                holderId,
                categoryId,
                costCenterId,
                effectiveFrom,
                effectiveTo,
                timezone);
        return ResponseEntity.ok(ApiResponse.of(service.build(query)));
    }

    @GetMapping("/drill-down")
    @PreAuthorize("hasAuthority('FINANCE_DASHBOARD_READ')")
    @Operation(summary = "Detalhamento de indicador do dashboard financeiro")
    public ResponseEntity<ApiResponse<List<DrillDownItem>>> drillDown(
            @RequestParam DashboardMetric metric,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false, defaultValue = DEFAULT_TZ) String timezone) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneId.of(timezone));
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfMonth(1);
        var query = new FinanceDashboardQuery(
                organizationId,
                storeId,
                storeGroupId,
                holderId,
                categoryId,
                costCenterId,
                effectiveFrom,
                effectiveTo,
                timezone);
        return ResponseEntity.ok(ApiResponse.of(service.drillDown(metric, query)));
    }
}
