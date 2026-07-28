package br.com.systemcommerce.pos.report.controller;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosExportFormat;
import br.com.systemcommerce.pos.report.dto.PosMetricSummary;
import br.com.systemcommerce.pos.report.dto.PosPeriodRow;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.pos.report.dto.PosReportType;
import br.com.systemcommerce.pos.report.service.PosReportService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Reports",
        description =
                """
                Relatórios do PDV: cálculos e filtros no backend, paginação server-side, exportação CSV \
                (PDF preparado). Escopo por loja quando o perfil exigir.
                """)
public class PosReportController {

    private final PosReportService posReportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('POS_REPORT_READ')")
    @Operation(summary = "Resumo métrico (ticket médio, itens, tempo médio)")
    public ResponseEntity<ApiResponse<PosMetricSummary>> summary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) Sale.SaleStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID customerId) {
        return ResponseEntity.ok(ApiResponse.of(posReportService.summary(filter(
                from, to, storeId, terminalId, operatorId, cashSessionId, null, status, productId, customerId))));
    }

    @GetMapping("/period/daily")
    @PreAuthorize("hasAuthority('POS_REPORT_READ')")
    @Operation(summary = "Vendas por período (dia)")
    public ResponseEntity<PageResponse<PosPeriodRow>> byPeriod(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID cashSessionId,
            @PageableDefault(size = 31) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posReportService.byPeriod(
                filter(from, to, storeId, terminalId, operatorId, cashSessionId, null, null, null, null),
                pageable)));
    }

    @GetMapping("/period/hourly")
    @PreAuthorize("hasAuthority('POS_REPORT_READ')")
    @Operation(summary = "Vendas por hora")
    public ResponseEntity<PageResponse<PosPeriodRow>> byHour(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID cashSessionId,
            @PageableDefault(size = 48) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posReportService.byHour(
                filter(from, to, storeId, terminalId, operatorId, cashSessionId, null, null, null, null),
                pageable)));
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('POS_REPORT_READ')")
    @Operation(summary = "Relatório agregado por tipo")
    public ResponseEntity<PageResponse<PosAggRow>> report(
            @PathVariable PosReportType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) Payment.PaymentMethod paymentMethod,
            @RequestParam(required = false) Sale.SaleStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posReportService.aggregate(
                type,
                filter(
                        from,
                        to,
                        storeId,
                        terminalId,
                        operatorId,
                        cashSessionId,
                        paymentMethod,
                        status,
                        productId,
                        customerId),
                pageable)));
    }

    @GetMapping(value = "/{type}/export", produces = {"text/csv", "application/pdf"})
    @PreAuthorize("hasAuthority('POS_REPORT_EXPORT')")
    @Operation(summary = "Exporta relatório (CSV ou PDF)")
    public ResponseEntity<byte[]> export(
            @PathVariable PosReportType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) Payment.PaymentMethod paymentMethod,
            @RequestParam(required = false) Sale.SaleStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "CSV") PosExportFormat format) {
        return posReportService.export(
                type,
                filter(
                        from,
                        to,
                        storeId,
                        terminalId,
                        operatorId,
                        cashSessionId,
                        paymentMethod,
                        status,
                        productId,
                        customerId),
                format);
    }

    private static PosReportFilter filter(
            Instant from,
            Instant to,
            UUID storeId,
            UUID terminalId,
            UUID operatorId,
            UUID cashSessionId,
            Payment.PaymentMethod paymentMethod,
            Sale.SaleStatus status,
            UUID productId,
            UUID customerId) {
        return new PosReportFilter(
                from,
                to,
                storeId,
                terminalId,
                operatorId,
                cashSessionId,
                paymentMethod,
                status,
                productId,
                customerId);
    }
}
