package br.com.systemcommerce.finance.report.controller;

import br.com.systemcommerce.finance.report.dto.FinanceReportDtos.*;
import br.com.systemcommerce.finance.report.service.FinanceReportExportService;
import br.com.systemcommerce.finance.report.service.FinanceReportService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance-reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Relatórios financeiros", description = "Consulta e exportação de relatórios financeiros (Prompt 118)")
public class FinanceReportController {

    private static final String DEFAULT_TZ = "America/Sao_Paulo";

    private final FinanceReportService reportService;
    private final FinanceReportExportService exportService;

    @GetMapping("/{reportType}")
    @PreAuthorize("hasAuthority('FINANCE_REPORT_READ')")
    @Operation(summary = "Consultar relatório financeiro paginado")
    public ResponseEntity<PageResponse<ReportRow>> query(
            @PathVariable ReportType reportType,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean detail,
            @RequestParam(required = false, defaultValue = DEFAULT_TZ) String timezone,
            @PageableDefault(size = 20) Pageable pageable) {
        var query = buildQuery(
                organizationId,
                storeId,
                holderId,
                categoryId,
                costCenterId,
                from,
                to,
                status,
                q,
                groupBy,
                sort,
                detail,
                timezone);
        return ResponseEntity.ok(PageResponse.from(reportService.query(reportType, query, pageable)));
    }

    @GetMapping(value = "/{reportType}/export")
    @PreAuthorize("hasAuthority('FINANCE_REPORT_EXPORT')")
    @Operation(summary = "Exportar relatório financeiro (CSV ou PDF)")
    public ResponseEntity<byte[]> export(
            @PathVariable ReportType reportType,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean detail,
            @RequestParam(required = false, defaultValue = DEFAULT_TZ) String timezone,
            @RequestParam(defaultValue = "csv") String format) {
        ExportFormat exportFormat = "pdf".equalsIgnoreCase(format) ? ExportFormat.PDF : ExportFormat.CSV;
        var query = buildQuery(
                organizationId,
                storeId,
                holderId,
                categoryId,
                costCenterId,
                from,
                to,
                status,
                q,
                groupBy,
                sort,
                detail,
                timezone);
        byte[] content = exportService.export(reportType, query, exportFormat);
        String filename = "finance-report-" + reportType.name().toLowerCase()
                + (exportFormat == ExportFormat.PDF ? ".pdf" : ".csv");
        MediaType mediaType =
                exportFormat == ExportFormat.PDF ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(content);
    }

    @GetMapping("/{reportType}/drill-down/{id}")
    @PreAuthorize("hasAuthority('FINANCE_REPORT_READ')")
    @Operation(summary = "Detalhe de registro do relatório financeiro")
    public ResponseEntity<ApiResponse<ReportRow>> drillDown(
            @PathVariable ReportType reportType,
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID holderId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID costCenterId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "true") boolean detail,
            @RequestParam(required = false, defaultValue = DEFAULT_TZ) String timezone) {
        var query = buildQuery(
                organizationId,
                storeId,
                holderId,
                categoryId,
                costCenterId,
                from,
                to,
                status,
                q,
                groupBy,
                sort,
                detail,
                timezone);
        return ResponseEntity.ok(ApiResponse.of(reportService.drillDown(reportType, id, query)));
    }

    private FinanceReportQuery buildQuery(
            UUID organizationId,
            UUID storeId,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            LocalDate from,
            LocalDate to,
            String status,
            String q,
            String groupBy,
            String sort,
            boolean detail,
            String timezone) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneId.of(timezone));
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfMonth(1);
        return new FinanceReportQuery(
                organizationId,
                storeId,
                holderId,
                categoryId,
                costCenterId,
                effectiveFrom,
                effectiveTo,
                status,
                q,
                groupBy,
                sort,
                detail,
                timezone);
    }
}
