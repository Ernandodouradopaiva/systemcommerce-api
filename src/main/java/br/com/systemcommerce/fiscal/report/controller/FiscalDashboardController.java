package br.com.systemcommerce.fiscal.report.controller;

import br.com.systemcommerce.fiscal.report.dto.FiscalDashboardSummary;
import br.com.systemcommerce.fiscal.report.service.FiscalDashboardService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Dashboard & Reports", description = "Dashboard e relatórios (Prompt 148)")
public class FiscalDashboardController {

    private final FiscalDashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('FISCAL_REPORT_READ') or hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<FiscalDashboardSummary>> dashboard(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.summary(organizationId, storeId, from, to)));
    }

    @GetMapping("/reports/{type}")
    @PreAuthorize("hasAuthority('FISCAL_REPORT_READ')")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> report(
            @PathVariable String type,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.report(type, organizationId, storeId, pageable)));
    }

    @GetMapping("/reports/{type}/export.csv")
    @PreAuthorize("hasAuthority('FISCAL_REPORT_READ')")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable String type,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(dashboardService.exportCsv(type, organizationId, storeId));
    }

    @GetMapping("/reports/{type}/export.pdf")
    @PreAuthorize("hasAuthority('FISCAL_REPORT_READ')")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable String type,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(dashboardService.exportPdfStub(type, organizationId, storeId));
    }
}
