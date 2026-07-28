package br.com.systemcommerce.report.controller;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.report.dto.AggregationReportRow;
import br.com.systemcommerce.report.dto.CustomerReportRow;
import br.com.systemcommerce.report.dto.InventoryReportRow;
import br.com.systemcommerce.report.dto.PaymentReportRow;
import br.com.systemcommerce.report.dto.SaleReportRow;
import br.com.systemcommerce.report.dto.StockMovementReportRow;
import br.com.systemcommerce.report.service.ReportService;
import br.com.systemcommerce.report.support.ReportScope;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "Reports",
        description =
                "Relatórios paginados com filtros no backend e exportação CSV. "
                        + "Períodos em Instant UTC (intervalo semiaberto [from, to)). "
                        + "Default: mês corrente UTC. Filtro opcional: storeId e scope=STORE|MULTI|GLOBAL.")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Vendas por período (detalhe)")
    public ResponseEntity<PageResponse<SaleReportRow>> sales(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Sale.SaleStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.sales(from, to, status, storeId, scope, pageable)));
    }

    @GetMapping(value = "/sales/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Exporta vendas por período em CSV")
    public ResponseEntity<byte[]> salesCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Sale.SaleStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("vendas.csv", reportService.salesCsv(from, to, status, storeId, scope));
    }

    @GetMapping("/sales/by-customer")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Vendas agregadas por cliente")
    public ResponseEntity<PageResponse<AggregationReportRow>> salesByCustomer(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.salesByCustomer(from, to, storeId, scope, pageable)));
    }

    @GetMapping(value = "/sales/by-customer/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> salesByCustomerCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("vendas-por-cliente.csv", reportService.salesByCustomerCsv(from, to, storeId, scope));
    }

    @GetMapping("/sales/by-product")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Vendas agregadas por produto")
    public ResponseEntity<PageResponse<AggregationReportRow>> salesByProduct(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.salesByProduct(from, to, storeId, scope, pageable)));
    }

    @GetMapping(value = "/sales/by-product/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> salesByProductCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("vendas-por-produto.csv", reportService.salesByProductCsv(from, to, storeId, scope));
    }

    @GetMapping("/sales/by-seller")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Vendas agregadas por vendedor")
    public ResponseEntity<PageResponse<AggregationReportRow>> salesBySeller(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.salesBySeller(from, to, storeId, scope, pageable)));
    }

    @GetMapping(value = "/sales/by-seller/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> salesBySellerCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("vendas-por-vendedor.csv", reportService.salesBySellerCsv(from, to, storeId, scope));
    }

    @GetMapping("/products/top-sold")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Produtos mais vendidos")
    public ResponseEntity<PageResponse<AggregationReportRow>> topSold(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.topSoldProducts(from, to, storeId, scope, pageable)));
    }

    @GetMapping("/inventory/current")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Estoque atual")
    public ResponseEntity<PageResponse<InventoryReportRow>> inventoryCurrent(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.inventoryCurrent(storeId, scope, pageable)));
    }

    @GetMapping(value = "/inventory/current/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> inventoryCurrentCsv(
            @RequestParam(required = false) UUID storeId, @RequestParam(required = false) ReportScope scope) {
        return csv("estoque-atual.csv", reportService.inventoryCurrentCsv(storeId, scope));
    }

    @GetMapping("/inventory/below-minimum")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Produtos abaixo do estoque mínimo")
    public ResponseEntity<PageResponse<InventoryReportRow>> inventoryBelowMinimum(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.inventoryBelowMinimum(storeId, scope, pageable)));
    }

    @GetMapping(value = "/inventory/below-minimum/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> inventoryBelowMinimumCsv(
            @RequestParam(required = false) UUID storeId, @RequestParam(required = false) ReportScope scope) {
        return csv("estoque-abaixo-minimo.csv", reportService.inventoryBelowMinimumCsv(storeId, scope));
    }

    @GetMapping("/inventory/movements")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Movimentações de estoque por período")
    public ResponseEntity<PageResponse<StockMovementReportRow>> stockMovements(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                reportService.stockMovements(from, to, type, productId, storeId, scope, pageable)));
    }

    @GetMapping(value = "/inventory/movements/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> stockMovementsCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv(
                "movimentacoes-estoque.csv",
                reportService.stockMovementsCsv(from, to, type, productId, storeId, scope));
    }

    @GetMapping("/payments")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Pagamentos por período")
    public ResponseEntity<PageResponse<PaymentReportRow>> payments(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Payment.PaymentMethod method,
            @RequestParam(required = false) Payment.PaymentStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.payments(from, to, method, status, storeId, scope, pageable)));
    }

    @GetMapping(value = "/payments/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> paymentsCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Payment.PaymentMethod method,
            @RequestParam(required = false) Payment.PaymentStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("pagamentos.csv", reportService.paymentsCsv(from, to, method, status, storeId, scope));
    }

    @GetMapping("/payments/by-method")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Pagamentos agregados por forma (somente CONFIRMED)")
    public ResponseEntity<PageResponse<AggregationReportRow>> paymentsByMethod(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(reportService.paymentsByMethod(from, to, storeId, scope, pageable)));
    }

    @GetMapping(value = "/payments/by-method/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> paymentsByMethodCsv(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return csv("pagamentos-por-forma.csv", reportService.paymentsByMethodCsv(from, to, storeId, scope));
    }

    @GetMapping("/customers")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    @Operation(summary = "Clientes cadastrados por período")
    public ResponseEntity<PageResponse<CustomerReportRow>> customers(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(reportService.customersByPeriod(from, to, pageable)));
    }

    @GetMapping(value = "/customers/csv", produces = "text/csv")
    @PreAuthorize(
            "hasAnyAuthority('REPORT_READ','REPORT_STORE_READ','REPORT_MULTI_STORE_READ','REPORT_GLOBAL_READ')")
    public ResponseEntity<byte[]> customersCsv(
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return csv("clientes.csv", reportService.customersByPeriodCsv(from, to));
    }

    private static ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }
}
