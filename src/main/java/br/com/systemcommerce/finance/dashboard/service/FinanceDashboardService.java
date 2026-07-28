package br.com.systemcommerce.finance.dashboard.service;

import br.com.systemcommerce.finance.dashboard.dto.FinanceDashboardDtos.*;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pricing.repository.StoreGroupMemberRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard financeiro — agregações calculadas exclusivamente no backend.
 * Cache em memória com TTL de 30 segundos (refresh controlado pelo {@code refreshedAt}).
 */
@Service
@RequiredArgsConstructor
public class FinanceDashboardService {

    private static final long CACHE_TTL_MS = 30_000L;
    private static final String DEFAULT_TZ = "America/Sao_Paulo";

    private final OrganizationService organizationService;
    private final StoreGroupMemberRepository storeGroupMemberRepository;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public FinanceDashboardResponse build(FinanceDashboardQuery query) {
        organizationService.requireUsable(query.organizationId());
        ResolvedFilters filters = resolveFilters(query);
        String cacheKey = filters.cacheKey();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAtMs() > System.currentTimeMillis()) {
            return cached.response();
        }
        FinanceDashboardResponse response = compute(filters);
        cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis() + CACHE_TTL_MS));
        return response;
    }

    @Transactional(readOnly = true)
    public List<DrillDownItem> drillDown(DashboardMetric metric, FinanceDashboardQuery query) {
        organizationService.requireUsable(query.organizationId());
        ResolvedFilters filters = resolveFilters(query);
        return switch (metric) {
            case AVAILABLE_BALANCE, BALANCES_BY_ACCOUNT -> drillBalancesByAccount(filters);
            case PAYABLES_DUE_TODAY -> drillPayablesDue(filters, filters.today(), filters.today());
            case RECEIVABLES_DUE_TODAY -> drillReceivablesDue(filters, filters.today(), filters.today());
            case OVERDUE_PAYABLES -> drillPayablesDue(filters, null, filters.today().minusDays(1));
            case OVERDUE_RECEIVABLES -> drillReceivablesDue(filters, null, filters.today().minusDays(1));
            case PAYMENTS_IN_PERIOD -> drillPayments(filters);
            case RECEIPTS_IN_PERIOD -> drillReceipts(filters);
            case EXPENSES_BY_CATEGORY -> drillExpensesByCategory(filters);
            case REVENUES_BY_CATEGORY -> drillRevenuesByCategory(filters);
            case CARDS_RECEIVABLE -> drillCardsReceivable(filters);
            case PENDING_RECONCILIATIONS -> drillPendingReconciliations(filters);
            case OPEN_CASH_SESSIONS -> drillOpenCashSessions(filters);
            case CASH_DIFFERENCES -> drillCashDifferences(filters);
            case TOP_SUPPLIERS_BY_PAYMENT -> drillTopSuppliers(filters);
            case TOP_CUSTOMERS_BY_BALANCE -> drillTopCustomers(filters);
        };
    }

    private FinanceDashboardResponse compute(ResolvedFilters f) {
        List<AccountBalance> balances = loadBalancesByAccount(f);
        BigDecimal availableBalance =
                balances.stream().map(AccountBalance::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payablesDueToday = sumPayablesDue(f, f.today(), f.today());
        BigDecimal receivablesDueToday = sumReceivablesDue(f, f.today(), f.today());
        BigDecimal overduePayables = sumPayablesDue(f, null, f.today().minusDays(1));
        BigDecimal overdueReceivables = sumReceivablesDue(f, null, f.today().minusDays(1));
        BigDecimal totalOpenReceivables = sumOpenReceivableBalance(f);
        BigDecimal delinquencyRate = totalOpenReceivables.compareTo(BigDecimal.ZERO) > 0
                ? overdueReceivables
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalOpenReceivables, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenues = sumRevenuesByCategory(f);
        BigDecimal expenses = sumExpensesByCategory(f);
        return new FinanceDashboardResponse(
                availableBalance,
                balances,
                payablesDueToday,
                receivablesDueToday,
                overduePayables,
                overdueReceivables,
                delinquencyRate,
                projectedCashFlow(f, 7),
                projectedCashFlow(f, 15),
                projectedCashFlow(f, 30),
                projectedCashFlow(f, 60),
                projectedCashFlow(f, 90),
                sumPaymentsInPeriod(f),
                sumReceiptsInPeriod(f),
                loadExpensesByCategory(f, 10),
                loadRevenuesByCategory(f, 10),
                revenues.subtract(expenses).setScale(2, RoundingMode.HALF_UP),
                sumCardsReceivable(f),
                countPendingReconciliations(f),
                countOpenCashSessions(f),
                sumCashDifferences(f),
                loadTopSuppliers(f, 5),
                loadTopCustomers(f, 5),
                Instant.now());
    }

    private ResolvedFilters resolveFilters(FinanceDashboardQuery query) {
        String tz = resolveTimezone(query.timezone());
        ZoneId zone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zone);
        LocalDate to = query.to() != null ? query.to() : today;
        LocalDate from = query.from() != null ? query.from() : to.withDayOfMonth(1);
        if (to.isBefore(from)) {
            throw new BusinessRuleException("Data final deve ser >= data inicial");
        }
        List<UUID> storeIds = null;
        if (query.storeGroupId() != null) {
            storeIds = storeGroupMemberRepository.findStoreIdsByStoreGroupId(query.storeGroupId());
            if (storeIds.isEmpty()) {
                storeIds = null;
            }
        }
        UUID effectiveStoreId = query.storeId();
        if (effectiveStoreId != null && storeIds != null && !storeIds.contains(effectiveStoreId)) {
            storeIds = List.of(effectiveStoreId);
        } else if (effectiveStoreId != null) {
            storeIds = null;
        }
        return new ResolvedFilters(
                query.organizationId(),
                effectiveStoreId,
                storeIds,
                query.holderId(),
                query.categoryId(),
                query.costCenterId(),
                from,
                to,
                tz,
                today);
    }

    @SuppressWarnings("unchecked")
    private List<AccountBalance> loadBalancesByAccount(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT h.id, h.code, h.name,
                  COALESCE(SUM(CASE WHEN m.active = TRUE AND m.reversed = FALSE THEN m.amount ELSE 0 END), 0)
                FROM financial_account_holders h
                LEFT JOIN financial_holder_movements m ON m.holder_id = h.id
                WHERE h.organization_id = :orgId AND h.active = TRUE
                """);
        appendHolderFilters(sql, f, "h");
        sql.append(" GROUP BY h.id, h.code, h.name ORDER BY h.code");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindHolderFilters(q, f, sql.toString());
        List<AccountBalance> result = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.add(new AccountBalance(
                    uuid(row[0]), str(row[1]), str(row[2]), money(row[3])));
        }
        return result;
    }

    private BigDecimal sumPayablesDue(ResolvedFilters f, LocalDate fromDue, LocalDate toDue) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(i.balance_amount), 0)
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                """);
        appendPayableFilters(sql, f);
        if (fromDue != null) {
            sql.append(" AND i.due_date >= :fromDue");
        }
        if (toDue != null) {
            sql.append(" AND i.due_date <= :toDue");
        }
        return money(runScalar(sql.toString(), f, fromDue, toDue));
    }

    private BigDecimal sumReceivablesDue(ResolvedFilters f, LocalDate fromDue, LocalDate toDue) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(i.balance_amount), 0)
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                """);
        appendReceivableFilters(sql, f);
        if (fromDue != null) {
            sql.append(" AND i.due_date >= :fromDue");
        }
        if (toDue != null) {
            sql.append(" AND i.due_date <= :toDue");
        }
        return money(runScalar(sql.toString(), f, fromDue, toDue));
    }

    private BigDecimal sumOpenReceivableBalance(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(i.balance_amount), 0)
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                """);
        appendReceivableFilters(sql, f);
        return money(runScalar(sql.toString(), f, null, null));
    }

    private BigDecimal projectedCashFlow(ResolvedFilters f, int days) {
        LocalDate start = f.today();
        LocalDate end = start.plusDays(days);
        BigDecimal inflows = sumProjectedReceivables(f, start, end)
                .add(sumProjectedCardSchedules(f, start, end));
        BigDecimal outflows = sumProjectedPayables(f, start, end);
        return inflows.subtract(outflows).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumProjectedReceivables(ResolvedFilters f, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(i.balance_amount), 0)
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :projFrom AND :projTo
                """);
        appendReceivableFilters(sql, f);
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("projFrom", from)
                .setParameter("projTo", to);
        bindReceivableFilters(q, f, sql.toString());
        return money(q.getSingleResult());
    }

    private BigDecimal sumProjectedPayables(ResolvedFilters f, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(i.balance_amount), 0)
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :projFrom AND :projTo
                """);
        appendPayableFilters(sql, f);
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("projFrom", from)
                .setParameter("projTo", to);
        bindPayableFilters(q, f, sql.toString());
        return money(q.getSingleResult());
    }

    private BigDecimal sumProjectedCardSchedules(ResolvedFilters f, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(s.net_amount), 0)
                FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE
                  AND s.status = 'SCHEDULED'
                  AND s.expected_date BETWEEN :projFrom AND :projTo
                """);
        appendStoreFilter(sql, f, "t");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("projFrom", from)
                .setParameter("projTo", to);
        bindStoreFilter(q, f, sql.toString());
        return money(q.getSingleResult());
    }

    private BigDecimal sumPaymentsInPeriod(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(ps.total_disbursed), 0)
                FROM payable_settlements ps
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.status = 'CONFIRMED'
                  AND ps.payment_date BETWEEN :from AND :to
                """);
        appendStoreFilter(sql, f, "ps");
        appendHolderFilterDirect(sql, f, "ps");
        return money(runPeriodScalar(sql.toString(), f));
    }

    private BigDecimal sumReceiptsInPeriod(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(rs.net_amount), 0)
                FROM receivable_settlements rs
                WHERE rs.organization_id = :orgId AND rs.active = TRUE
                  AND rs.status = 'CONFIRMED'
                  AND rs.payment_date BETWEEN :from AND :to
                """);
        appendStoreFilter(sql, f, "rs");
        appendHolderFilterDirect(sql, f, "rs");
        return money(runPeriodScalar(sql.toString(), f));
    }

    @SuppressWarnings("unchecked")
    private List<CategoryAmount> loadExpensesByCategory(ResolvedFilters f, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT e.financial_category_id, COALESCE(c.name, 'Sem categoria'),
                  COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                LEFT JOIN financial_categories c ON c.id = e.financial_category_id
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type IN ('MANUAL_EXPENSE','FEE','TAX','CORRECTION')
                  AND e.entry_date BETWEEN :from AND :to
                """);
        appendEntryFilters(sql, f);
        sql.append(" GROUP BY e.financial_category_id, c.name ORDER BY 3 DESC LIMIT ").append(limit);
        return mapCategoryAmounts(sql.toString(), f);
    }

    @SuppressWarnings("unchecked")
    private List<CategoryAmount> loadRevenuesByCategory(ResolvedFilters f, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT e.financial_category_id, COALESCE(c.name, 'Sem categoria'),
                  COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                LEFT JOIN financial_categories c ON c.id = e.financial_category_id
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type IN ('MANUAL_REVENUE','YIELD','ADJUSTMENT')
                  AND e.entry_date BETWEEN :from AND :to
                """);
        appendEntryFilters(sql, f);
        sql.append(" GROUP BY e.financial_category_id, c.name ORDER BY 3 DESC LIMIT ").append(limit);
        return mapCategoryAmounts(sql.toString(), f);
    }

    private BigDecimal sumExpensesByCategory(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type IN ('MANUAL_EXPENSE','FEE','TAX','CORRECTION')
                  AND e.entry_date BETWEEN :from AND :to
                """);
        appendEntryFilters(sql, f);
        return money(runPeriodScalar(sql.toString(), f));
    }

    private BigDecimal sumRevenuesByCategory(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type IN ('MANUAL_REVENUE','YIELD','ADJUSTMENT')
                  AND e.entry_date BETWEEN :from AND :to
                """);
        appendEntryFilters(sql, f);
        return money(runPeriodScalar(sql.toString(), f));
    }

    private BigDecimal sumCardsReceivable(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(s.net_amount), 0)
                FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE AND s.status = 'SCHEDULED'
                """);
        appendStoreFilter(sql, f, "t");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindStoreFilter(q, f, sql.toString());
        return money(q.getSingleResult());
    }

    private long countPendingReconciliations(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*)
                FROM bank_statement_entries bse
                JOIN financial_account_holders h ON h.id = bse.holder_id
                WHERE h.organization_id = :orgId AND bse.active = TRUE
                  AND bse.reconciliation_status = 'UNMATCHED'
                """);
        appendHolderFilters(sql, f, "h");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindHolderFilters(q, f, sql.toString());
        return ((Number) q.getSingleResult()).longValue();
    }

    private long countOpenCashSessions(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*)
                FROM cash_sessions cs
                JOIN stores s ON s.id = cs.store_id
                WHERE s.organization_id = :orgId AND cs.active = TRUE
                  AND cs.status IN ('OPEN','CLOSING')
                """);
        appendStoreFilter(sql, f, "cs");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindStoreFilter(q, f, sql.toString());
        return ((Number) q.getSingleResult()).longValue();
    }

    private BigDecimal sumCashDifferences(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(ABS(cs.difference_amount)), 0)
                FROM cash_sessions cs
                JOIN stores s ON s.id = cs.store_id
                WHERE s.organization_id = :orgId AND cs.active = TRUE
                  AND cs.difference_amount IS NOT NULL
                """);
        appendStoreFilter(sql, f, "cs");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindStoreFilter(q, f, sql.toString());
        return money(q.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<RankedEntity> loadTopSuppliers(ResolvedFilters f, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT sup.id, COALESCE(sup.trade_name, sup.legal_name),
                  COALESCE(SUM(ps.total_disbursed), 0)
                FROM payable_settlements ps
                JOIN payable_settlement_allocations psa ON psa.settlement_id = ps.id
                JOIN payable_installments pi ON pi.id = psa.installment_id
                JOIN payables p ON p.id = pi.payable_id
                JOIN suppliers sup ON sup.id = p.supplier_id
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.status = 'CONFIRMED'
                  AND ps.payment_date BETWEEN :from AND :to
                """);
        appendStoreFilter(sql, f, "ps");
        sql.append(" GROUP BY sup.id, sup.trade_name, sup.legal_name ORDER BY 3 DESC LIMIT ").append(limit);
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("from", f.from())
                .setParameter("to", f.to());
        bindStoreFilter(q, f, sql.toString());
        List<RankedEntity> result = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.add(new RankedEntity(uuid(row[0]), str(row[1]), money(row[2])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<RankedEntity> loadTopCustomers(ResolvedFilters f, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT c.id, COALESCE(c.trade_name, c.name),
                  COALESCE(SUM(r.balance_amount), 0)
                FROM receivables r
                JOIN customers c ON c.id = r.customer_id
                WHERE r.organization_id = :orgId AND r.active = TRUE
                  AND r.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                """);
        appendReceivableFilters(sql, f);
        sql.append(" GROUP BY c.id, c.trade_name, c.name ORDER BY 3 DESC LIMIT ").append(limit);
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindReceivableFilters(q, f, sql.toString());
        List<RankedEntity> result = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.add(new RankedEntity(uuid(row[0]), str(row[1]), money(row[2])));
        }
        return result;
    }

    private List<DrillDownItem> drillBalancesByAccount(ResolvedFilters f) {
        return loadBalancesByAccount(f).stream()
                .map(b -> new DrillDownItem(b.holderId(), b.name(), b.balance(), f.today()))
                .toList();
    }

    private List<DrillDownItem> drillPayablesDue(ResolvedFilters f, LocalDate fromDue, LocalDate toDue) {
        return drillInstallments(
                """
                SELECT i.id, COALESCE(p.document_number, 'Conta a pagar'), i.balance_amount, i.due_date
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                """,
                f,
                fromDue,
                toDue,
                true);
    }

    private List<DrillDownItem> drillReceivablesDue(ResolvedFilters f, LocalDate fromDue, LocalDate toDue) {
        return drillInstallments(
                """
                SELECT i.id, COALESCE(r.document_number, 'Conta a receber'), i.balance_amount, i.due_date
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                """,
                f,
                fromDue,
                toDue,
                false);
    }

    private List<DrillDownItem> drillInstallments(
            String baseSql, ResolvedFilters f, LocalDate fromDue, LocalDate toDue, boolean payable) {
        StringBuilder sql = new StringBuilder(baseSql);
        if (payable) {
            appendPayableFilters(sql, f);
        } else {
            appendReceivableFilters(sql, f);
        }
        if (fromDue != null) {
            sql.append(" AND i.due_date >= :fromDue");
        }
        if (toDue != null) {
            sql.append(" AND i.due_date <= :toDue");
        }
        sql.append(" ORDER BY i.due_date LIMIT 200");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        if (payable) {
            bindPayableFilters(q, f, sql.toString());
        } else {
            bindReceivableFilters(q, f, sql.toString());
        }
        if (fromDue != null) {
            q.setParameter("fromDue", fromDue);
        }
        if (toDue != null) {
            q.setParameter("toDue", toDue);
        }
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillPayments(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT ps.id, COALESCE(ps.reference_code, 'Pagamento'), ps.total_disbursed, ps.payment_date
                FROM payable_settlements ps
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.status = 'CONFIRMED'
                  AND ps.payment_date BETWEEN :from AND :to
                """);
        appendStoreFilter(sql, f, "ps");
        sql.append(" ORDER BY ps.payment_date DESC LIMIT 200");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("from", f.from())
                .setParameter("to", f.to());
        bindStoreFilter(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillReceipts(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT rs.id, COALESCE(rs.reference_code, 'Recebimento'), rs.net_amount, rs.payment_date
                FROM receivable_settlements rs
                WHERE rs.organization_id = :orgId AND rs.active = TRUE
                  AND rs.status = 'CONFIRMED'
                  AND rs.payment_date BETWEEN :from AND :to
                """);
        appendStoreFilter(sql, f, "rs");
        sql.append(" ORDER BY rs.payment_date DESC LIMIT 200");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("from", f.from())
                .setParameter("to", f.to());
        bindStoreFilter(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillExpensesByCategory(ResolvedFilters f) {
        return loadExpensesByCategory(f, 50).stream()
                .map(c -> new DrillDownItem(c.categoryId(), c.categoryName(), c.amount(), f.to()))
                .toList();
    }

    private List<DrillDownItem> drillRevenuesByCategory(ResolvedFilters f) {
        return loadRevenuesByCategory(f, 50).stream()
                .map(c -> new DrillDownItem(c.categoryId(), c.categoryName(), c.amount(), f.to()))
                .toList();
    }

    private List<DrillDownItem> drillCardsReceivable(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT s.id, CONCAT('Cartão parcela ', s.installment_number), s.net_amount, s.expected_date
                FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE AND s.status = 'SCHEDULED'
                """);
        appendStoreFilter(sql, f, "t");
        sql.append(" ORDER BY s.expected_date LIMIT 200");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindStoreFilter(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillPendingReconciliations(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT bse.id, bse.description, bse.amount, bse.entry_date
                FROM bank_statement_entries bse
                JOIN financial_account_holders h ON h.id = bse.holder_id
                WHERE h.organization_id = :orgId AND bse.active = TRUE
                  AND bse.reconciliation_status = 'UNMATCHED'
                """);
        appendHolderFilters(sql, f, "h");
        sql.append(" ORDER BY bse.entry_date DESC LIMIT 200");
        Query q = em.createNativeQuery(sql.toString()).setParameter("orgId", f.organizationId());
        bindHolderFilters(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillOpenCashSessions(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT cs.id, CONCAT('Sessão caixa ', cs.id), cs.opening_amount, (cs.opened_at AT TIME ZONE :tz)::date
                FROM cash_sessions cs
                JOIN stores s ON s.id = cs.store_id
                WHERE s.organization_id = :orgId AND cs.active = TRUE
                  AND cs.status IN ('OPEN','CLOSING')
                """);
        appendStoreFilter(sql, f, "cs");
        sql.append(" ORDER BY cs.opened_at DESC LIMIT 200");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("tz", f.timezone());
        bindStoreFilter(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillCashDifferences(ResolvedFilters f) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT cs.id, CONCAT('Diferença sessão ', cs.id), ABS(cs.difference_amount), (cs.closed_at AT TIME ZONE :tz)::date
                FROM cash_sessions cs
                JOIN stores s ON s.id = cs.store_id
                WHERE s.organization_id = :orgId AND cs.active = TRUE
                  AND cs.difference_amount IS NOT NULL AND cs.difference_amount <> 0
                """);
        appendStoreFilter(sql, f, "cs");
        sql.append(" ORDER BY cs.closed_at DESC NULLS LAST LIMIT 200");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", f.organizationId())
                .setParameter("tz", f.timezone());
        bindStoreFilter(q, f, sql.toString());
        return mapDrillDown(q.getResultList());
    }

    private List<DrillDownItem> drillTopSuppliers(ResolvedFilters f) {
        return loadTopSuppliers(f, 50).stream()
                .map(s -> new DrillDownItem(s.id(), s.label(), s.amount(), f.to()))
                .toList();
    }

    private List<DrillDownItem> drillTopCustomers(ResolvedFilters f) {
        return loadTopCustomers(f, 50).stream()
                .map(c -> new DrillDownItem(c.id(), c.label(), c.amount(), f.today()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<CategoryAmount> mapCategoryAmounts(String sql, ResolvedFilters f) {
        Query q = em.createNativeQuery(sql)
                .setParameter("orgId", f.organizationId())
                .setParameter("from", f.from())
                .setParameter("to", f.to());
        bindEntryFilters(q, f, sql);
        List<CategoryAmount> result = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.add(new CategoryAmount(uuid(row[0]), str(row[1]), money(row[2])));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<DrillDownItem> mapDrillDown(List<?> rows) {
        List<DrillDownItem> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            LocalDate date = cols[3] instanceof java.sql.Date d
                    ? d.toLocalDate()
                    : cols[3] != null ? LocalDate.parse(cols[3].toString()) : null;
            result.add(new DrillDownItem(uuid(cols[0]), str(cols[1]), money(cols[2]), date));
        }
        return result;
    }

    private Object runScalar(String sql, ResolvedFilters f, LocalDate fromDue, LocalDate toDue) {
        Query q = em.createNativeQuery(sql).setParameter("orgId", f.organizationId());
        if (sql.contains("payables") || sql.contains("payable_installments")) {
            bindPayableFilters(q, f, sql);
        } else {
            bindReceivableFilters(q, f, sql);
        }
        if (fromDue != null) {
            q.setParameter("fromDue", fromDue);
        }
        if (toDue != null) {
            q.setParameter("toDue", toDue);
        }
        return q.getSingleResult();
    }

    private Object runPeriodScalar(String sql, ResolvedFilters f) {
        Query q = em.createNativeQuery(sql)
                .setParameter("orgId", f.organizationId())
                .setParameter("from", f.from())
                .setParameter("to", f.to());
        if (sql.contains("financial_entries")) {
            bindEntryFilters(q, f, sql);
        } else {
            bindStoreFilter(q, f, sql);
            bindHolderFilterDirect(q, f, sql);
        }
        return q.getSingleResult();
    }

    private void appendStoreFilter(StringBuilder sql, ResolvedFilters f, String alias) {
        if (f.storeId() != null) {
            sql.append(" AND ").append(alias).append(".store_id = :storeId");
        } else if (f.storeIds() != null && !f.storeIds().isEmpty()) {
            sql.append(" AND ").append(alias).append(".store_id IN (:storeIds)");
        }
    }

    private void appendHolderFilters(StringBuilder sql, ResolvedFilters f, String alias) {
        appendStoreFilter(sql, f, alias);
        if (f.holderId() != null) {
            sql.append(" AND ").append(alias).append(".id = :holderId");
        }
    }

    private void appendHolderFilterDirect(StringBuilder sql, ResolvedFilters f, String alias) {
        if (f.holderId() != null) {
            sql.append(" AND ").append(alias).append(".holder_id = :holderId");
        }
    }

    private void appendPayableFilters(StringBuilder sql, ResolvedFilters f) {
        appendStoreFilter(sql, f, "p");
        if (f.categoryId() != null) {
            sql.append(" AND p.financial_category_id = :categoryId");
        }
        if (f.costCenterId() != null) {
            sql.append(" AND p.cost_center_id = :costCenterId");
        }
    }

    private void appendReceivableFilters(StringBuilder sql, ResolvedFilters f) {
        appendStoreFilter(sql, f, "r");
        if (f.categoryId() != null) {
            sql.append(" AND r.financial_category_id = :categoryId");
        }
        if (f.costCenterId() != null) {
            sql.append(" AND r.cost_center_id = :costCenterId");
        }
    }

    private void appendEntryFilters(StringBuilder sql, ResolvedFilters f) {
        appendStoreFilter(sql, f, "e");
        appendHolderFilterDirect(sql, f, "e");
        if (f.categoryId() != null) {
            sql.append(" AND e.financial_category_id = :categoryId");
        }
        if (f.costCenterId() != null) {
            sql.append(" AND e.cost_center_id = :costCenterId");
        }
    }

    private void bindStoreFilter(Query q, ResolvedFilters f, String sql) {
        if (sql.contains(":storeId") && f.storeId() != null) {
            q.setParameter("storeId", f.storeId());
        }
        if (sql.contains(":storeIds") && f.storeIds() != null && !f.storeIds().isEmpty()) {
            q.setParameter("storeIds", f.storeIds());
        }
    }

    private void bindHolderFilters(Query q, ResolvedFilters f, String sql) {
        bindStoreFilter(q, f, sql);
        if (sql.contains(":holderId") && f.holderId() != null) {
            q.setParameter("holderId", f.holderId());
        }
    }

    private void bindHolderFilterDirect(Query q, ResolvedFilters f, String sql) {
        if (sql.contains(":holderId") && f.holderId() != null) {
            q.setParameter("holderId", f.holderId());
        }
    }

    private void bindPayableFilters(Query q, ResolvedFilters f, String sql) {
        bindStoreFilter(q, f, sql);
        if (sql.contains(":categoryId") && f.categoryId() != null) {
            q.setParameter("categoryId", f.categoryId());
        }
        if (sql.contains(":costCenterId") && f.costCenterId() != null) {
            q.setParameter("costCenterId", f.costCenterId());
        }
    }

    private void bindReceivableFilters(Query q, ResolvedFilters f, String sql) {
        bindStoreFilter(q, f, sql);
        if (sql.contains(":categoryId") && f.categoryId() != null) {
            q.setParameter("categoryId", f.categoryId());
        }
        if (sql.contains(":costCenterId") && f.costCenterId() != null) {
            q.setParameter("costCenterId", f.costCenterId());
        }
    }

    private void bindEntryFilters(Query q, ResolvedFilters f, String sql) {
        bindStoreFilter(q, f, sql);
        bindHolderFilterDirect(q, f, sql);
        if (sql.contains(":categoryId") && f.categoryId() != null) {
            q.setParameter("categoryId", f.categoryId());
        }
        if (sql.contains(":costCenterId") && f.costCenterId() != null) {
            q.setParameter("costCenterId", f.costCenterId());
        }
    }

    private String resolveTimezone(String timezone) {
        return timezone != null && !timezone.isBlank() ? timezone : DEFAULT_TZ;
    }

    private BigDecimal money(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private UUID uuid(Object value) {
        return value != null ? UUID.fromString(value.toString()) : null;
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private record CacheEntry(FinanceDashboardResponse response, long expiresAtMs) {}

    private record ResolvedFilters(
            UUID organizationId,
            UUID storeId,
            List<UUID> storeIds,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            LocalDate from,
            LocalDate to,
            String timezone,
            LocalDate today) {

        String cacheKey() {
            return organizationId + "|"
                    + Objects.toString(storeId, "")
                    + "|"
                    + Objects.toString(storeIds, "")
                    + "|"
                    + Objects.toString(holderId, "")
                    + "|"
                    + Objects.toString(categoryId, "")
                    + "|"
                    + Objects.toString(costCenterId, "")
                    + "|"
                    + from + "|" + to + "|" + timezone;
        }
    }
}
