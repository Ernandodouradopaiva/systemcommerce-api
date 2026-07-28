package br.com.systemcommerce.dashboard.executive.controller;

import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardQuery;
import br.com.systemcommerce.dashboard.executive.dto.ExecutiveDashboardResponse;
import br.com.systemcommerce.dashboard.executive.dto.ExecutivePerspective;
import br.com.systemcommerce.dashboard.executive.service.ExecutiveDashboardService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/executive")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Executive Dashboard", description = "Dashboard executivo profissional (Prompt 87)")
public class ExecutiveDashboardController {

    private final ExecutiveDashboardService executiveDashboardService;

    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('EXECUTIVE_DASHBOARD_READ','DASHBOARD_READ','DASHBOARD_GLOBAL_READ','DASHBOARD_STORE_READ')")
    @Operation(summary = "Indicadores executivos consolidados (cálculo na API, cache controlado)")
    public ResponseEntity<ApiResponse<ExecutiveDashboardResponse>> summary(
            @RequestParam(required = false) ExecutivePerspective perspective,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "UTC") String timezone) {
        var query = new ExecutiveDashboardQuery(
                perspective, organizationId, storeGroupId, storeId, warehouseId, channelCode, from, to, timezone);
        return ResponseEntity.ok(ApiResponse.of(executiveDashboardService.build(query)));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('EXECUTIVE_DASHBOARD_EXPORT','DASHBOARD_GLOBAL_READ')")
    @Operation(summary = "Exportação CSV dos indicadores executivos")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) ExecutivePerspective perspective,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "UTC") String timezone) {
        var query = new ExecutiveDashboardQuery(
                perspective, organizationId, storeGroupId, storeId, warehouseId, channelCode, from, to, timezone);
        byte[] csv = executiveDashboardService.exportCsv(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=executive-dashboard.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
