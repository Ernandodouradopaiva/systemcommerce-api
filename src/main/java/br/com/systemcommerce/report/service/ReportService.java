package br.com.systemcommerce.report.service;

import br.com.systemcommerce.dashboard.service.DashboardService;
import br.com.systemcommerce.report.dto.AggregationReportRow;
import br.com.systemcommerce.report.dto.CustomerReportRow;
import br.com.systemcommerce.report.dto.InventoryReportRow;
import br.com.systemcommerce.report.dto.PaymentReportRow;
import br.com.systemcommerce.report.dto.SaleReportRow;
import br.com.systemcommerce.report.dto.StockMovementReportRow;
import br.com.systemcommerce.report.repository.ReportQueryRepository;
import br.com.systemcommerce.report.support.CsvWriter;
import br.com.systemcommerce.report.support.ReportPeriodUtils;
import br.com.systemcommerce.report.support.ReportRowMapper;
import br.com.systemcommerce.report.support.ReportScope;
import br.com.systemcommerce.report.support.ReportStoreAccessSupport;
import br.com.systemcommerce.report.support.ReportStoreFilter;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios ERP paginados e exportação CSV.
 * <p>
 * Endpoints aceitam {@code storeId} e {@code scope} ({@link ReportScope#STORE STORE},
 * {@link ReportScope#MULTI MULTI}, {@link ReportScope#GLOBAL GLOBAL}). Usuários sem escopo global
 * consultam apenas lojas acessíveis via {@link StoreAuthorizationEvaluator}.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int CSV_MAX_ROWS = 10_000;

    private final ReportQueryRepository reportQueryRepository;
    private final ReportStoreAccessSupport reportStoreAccessSupport;

    @Transactional(readOnly = true)
    public Page<SaleReportRow> sales(
            Instant from,
            Instant to,
            Sale.SaleStatus status,
            UUID storeId,
            ReportScope scope,
            Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        var nativeAllowed = filter.nativeAllowedStores();
        return reportQueryRepository
                .salesDetail(
                        status == null ? null : status.name(),
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        nativeAllowed.restrict(),
                        nativeAllowed.ids(),
                        pageable)
                .map(ReportRowMapper::toSaleRow);
    }

    @Transactional(readOnly = true)
    public byte[] salesCsv(
            Instant from, Instant to, Sale.SaleStatus status, UUID storeId, ReportScope scope) {
        Page<SaleReportRow> page = sales(from, to, status, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (SaleReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.saleNumber()),
                    nullToEmpty(row.saleDate()),
                    nullToEmpty(row.status()),
                    nullToEmpty(row.totalAmount()),
                    nullToEmpty(row.customerName()),
                    nullToEmpty(row.sellerName())));
        }
        return CsvWriter.write(
                List.of("Numero", "Data", "Status", "Total", "Cliente", "Vendedor"), rows);
    }

    @Transactional(readOnly = true)
    public Page<AggregationReportRow> salesByCustomer(
            Instant from, Instant to, UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        List<AggregationReportRow> all = reportQueryRepository
                .topCustomers(
                        DashboardService.EFFECTIVE_SALE_STATUSES,
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        filter.allowedStoreIds(),
                        PageRequest.of(0, CSV_MAX_ROWS))
                .stream()
                .map(ReportRowMapper::toCustomerAgg)
                .toList();
        return toPage(all, pageable);
    }

    @Transactional(readOnly = true)
    public byte[] salesByCustomerCsv(Instant from, Instant to, UUID storeId, ReportScope scope) {
        return aggregationCsv(
                salesByCustomer(from, to, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS)).getContent(),
                List.of("ClienteId", "Documento", "Nome", "QtdVendas", "Total"));
    }

    @Transactional(readOnly = true)
    public Page<AggregationReportRow> salesByProduct(
            Instant from, Instant to, UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        List<AggregationReportRow> all = reportQueryRepository
                .topProducts(
                        DashboardService.EFFECTIVE_SALE_STATUSES,
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        filter.allowedStoreIds(),
                        PageRequest.of(0, CSV_MAX_ROWS))
                .stream()
                .map(ReportRowMapper::toProductAgg)
                .toList();
        return toPage(all, pageable);
    }

    @Transactional(readOnly = true)
    public byte[] salesByProductCsv(Instant from, Instant to, UUID storeId, ReportScope scope) {
        List<AggregationReportRow> rows =
                salesByProduct(from, to, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS)).getContent();
        List<List<String>> csv = new ArrayList<>();
        for (AggregationReportRow row : rows) {
            csv.add(List.of(
                    nullToEmpty(row.id()),
                    nullToEmpty(row.code()),
                    nullToEmpty(row.name()),
                    nullToEmpty(row.quantity()),
                    nullToEmpty(row.amount())));
        }
        return CsvWriter.write(List.of("ProdutoId", "SKU", "Nome", "Quantidade", "Total"), csv);
    }

    @Transactional(readOnly = true)
    public Page<AggregationReportRow> salesBySeller(
            Instant from, Instant to, UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        List<AggregationReportRow> all = reportQueryRepository
                .salesBySeller(
                        DashboardService.EFFECTIVE_SALE_STATUSES,
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        filter.allowedStoreIds(),
                        PageRequest.of(0, CSV_MAX_ROWS))
                .stream()
                .map(ReportRowMapper::toSellerAgg)
                .toList();
        return toPage(all, pageable);
    }

    @Transactional(readOnly = true)
    public byte[] salesBySellerCsv(Instant from, Instant to, UUID storeId, ReportScope scope) {
        return aggregationCsv(
                salesBySeller(from, to, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS)).getContent(),
                List.of("VendedorId", "Codigo", "Nome", "QtdVendas", "Total"));
    }

    @Transactional(readOnly = true)
    public Page<AggregationReportRow> topSoldProducts(
            Instant from, Instant to, UUID storeId, ReportScope scope, Pageable pageable) {
        return salesByProduct(from, to, storeId, scope, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryReportRow> inventoryCurrent(UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var nativeAllowed = filter.nativeAllowedStores();
        return reportQueryRepository
                .inventoryCurrent(
                        filter.storeId(),
                        nativeAllowed.restrict(),
                        nativeAllowed.ids(),
                        pageable)
                .map(ReportRowMapper::toInventoryRow);
    }

    @Transactional(readOnly = true)
    public byte[] inventoryCurrentCsv(UUID storeId, ReportScope scope) {
        Page<InventoryReportRow> page = inventoryCurrent(storeId, scope, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (InventoryReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.productSku()),
                    nullToEmpty(row.productName()),
                    nullToEmpty(row.quantity()),
                    nullToEmpty(row.minStock()),
                    nullToEmpty(row.unitOfMeasure())));
        }
        return CsvWriter.write(List.of("SKU", "Produto", "Saldo", "Minimo", "Unidade"), rows);
    }

    @Transactional(readOnly = true)
    public Page<InventoryReportRow> inventoryBelowMinimum(UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var nativeAllowed = filter.nativeAllowedStores();
        return reportQueryRepository
                .inventoryBelowMinimum(
                        filter.storeId(),
                        nativeAllowed.restrict(),
                        nativeAllowed.ids(),
                        pageable)
                .map(ReportRowMapper::toInventoryRow);
    }

    @Transactional(readOnly = true)
    public byte[] inventoryBelowMinimumCsv(UUID storeId, ReportScope scope) {
        Page<InventoryReportRow> page = inventoryBelowMinimum(storeId, scope, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (InventoryReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.productSku()),
                    nullToEmpty(row.productName()),
                    nullToEmpty(row.quantity()),
                    nullToEmpty(row.minStock())));
        }
        return CsvWriter.write(List.of("SKU", "Produto", "Saldo", "Minimo"), rows);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementReportRow> stockMovements(
            Instant from,
            Instant to,
            String type,
            UUID productId,
            UUID storeId,
            ReportScope scope,
            Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        var nativeAllowed = filter.nativeAllowedStores();
        return reportQueryRepository
                .stockMovements(
                        blankToNull(type),
                        productId == null ? null : productId.toString(),
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        nativeAllowed.restrict(),
                        nativeAllowed.ids(),
                        pageable)
                .map(ReportRowMapper::toMovementRow);
    }

    @Transactional(readOnly = true)
    public byte[] stockMovementsCsv(
            Instant from, Instant to, String type, UUID productId, UUID storeId, ReportScope scope) {
        Page<StockMovementReportRow> page =
                stockMovements(from, to, type, productId, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (StockMovementReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.createdAt()),
                    nullToEmpty(row.productSku()),
                    nullToEmpty(row.productName()),
                    nullToEmpty(row.type()),
                    nullToEmpty(row.quantity()),
                    nullToEmpty(row.previousQuantity()),
                    nullToEmpty(row.newQuantity()),
                    nullToEmpty(row.referenceType())));
        }
        return CsvWriter.write(
                List.of("Data", "SKU", "Produto", "Tipo", "Qtd", "Anterior", "Posterior", "Origem"), rows);
    }

    @Transactional(readOnly = true)
    public Page<PaymentReportRow> payments(
            Instant from,
            Instant to,
            Payment.PaymentMethod method,
            Payment.PaymentStatus status,
            UUID storeId,
            ReportScope scope,
            Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        var nativeAllowed = filter.nativeAllowedStores();
        return reportQueryRepository
                .paymentsDetail(
                        method == null ? null : method.name(),
                        status == null ? null : status.name(),
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        nativeAllowed.restrict(),
                        nativeAllowed.ids(),
                        pageable)
                .map(ReportRowMapper::toPaymentRow);
    }

    @Transactional(readOnly = true)
    public byte[] paymentsCsv(
            Instant from,
            Instant to,
            Payment.PaymentMethod method,
            Payment.PaymentStatus status,
            UUID storeId,
            ReportScope scope) {
        Page<PaymentReportRow> page =
                payments(from, to, method, status, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (PaymentReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.paidAt()),
                    nullToEmpty(row.saleNumber()),
                    nullToEmpty(row.method()),
                    nullToEmpty(row.amount()),
                    nullToEmpty(row.status()),
                    nullToEmpty(row.externalReference())));
        }
        return CsvWriter.write(
                List.of("Data", "Venda", "Forma", "Valor", "Status", "Referencia"), rows);
    }

    @Transactional(readOnly = true)
    public Page<AggregationReportRow> paymentsByMethod(
            Instant from, Instant to, UUID storeId, ReportScope scope, Pageable pageable) {
        ReportStoreFilter filter = reportStoreAccessSupport.resolveReportFilter(storeId, scope);
        if (filter.isEmpty()) {
            return Page.empty(pageable);
        }
        var range = defaultRange(from, to);
        List<AggregationReportRow> all = reportQueryRepository
                .paymentsByMethod(
                        Payment.PaymentStatus.CONFIRMED,
                        range.from(),
                        range.toExclusive(),
                        filter.storeId(),
                        filter.allowedStoreIds())
                .stream()
                .map(ReportRowMapper::toMethodAgg)
                .toList();
        return toPage(all, pageable);
    }

    @Transactional(readOnly = true)
    public byte[] paymentsByMethodCsv(Instant from, Instant to, UUID storeId, ReportScope scope) {
        return aggregationCsv(
                paymentsByMethod(from, to, storeId, scope, PageRequest.of(0, CSV_MAX_ROWS)).getContent(),
                List.of("Id", "Codigo", "Forma", "Qtd", "Total"));
    }

    @Transactional(readOnly = true)
    public Page<CustomerReportRow> customersByPeriod(Instant from, Instant to, Pageable pageable) {
        var range = defaultRange(from, to);
        return reportQueryRepository
                .customersByPeriod(range.from(), range.toExclusive(), pageable)
                .map(ReportRowMapper::toCustomerRow);
    }

    @Transactional(readOnly = true)
    public byte[] customersByPeriodCsv(Instant from, Instant to) {
        Page<CustomerReportRow> page = customersByPeriod(from, to, PageRequest.of(0, CSV_MAX_ROWS));
        List<List<String>> rows = new ArrayList<>();
        for (CustomerReportRow row : page.getContent()) {
            rows.add(List.of(
                    nullToEmpty(row.createdAt()),
                    nullToEmpty(row.name()),
                    nullToEmpty(row.document()),
                    nullToEmpty(row.type()),
                    nullToEmpty(row.status())));
        }
        return CsvWriter.write(List.of("Cadastro", "Nome", "Documento", "Tipo", "Status"), rows);
    }

    private ReportPeriodUtils.InstantRange defaultRange(Instant from, Instant to) {
        LocalDate today = ReportPeriodUtils.todayUtc();
        var month = ReportPeriodUtils.monthRange(today);
        return ReportPeriodUtils.resolve(from, to, month.from(), month.toExclusive());
    }

    private static <T> Page<T> toPage(List<T> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    private static byte[] aggregationCsv(List<AggregationReportRow> content, List<String> headers) {
        List<List<String>> rows = new ArrayList<>();
        for (AggregationReportRow row : content) {
            rows.add(List.of(
                    nullToEmpty(row.id()),
                    nullToEmpty(row.code()),
                    nullToEmpty(row.name()),
                    nullToEmpty(row.count()),
                    nullToEmpty(row.amount())));
        }
        return CsvWriter.write(headers, rows);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
