package br.com.systemcommerce.finance.report.service;

import br.com.systemcommerce.finance.report.dto.FinanceReportDtos.*;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceReportService {

    private static final String DEFAULT_TZ = "America/Sao_Paulo";

    private final OrganizationService organizationService;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<ReportRow> query(ReportType reportType, FinanceReportQuery query, Pageable pageable) {
        organizationService.requireUsable(query.organizationId());
        ResolvedFilters f = resolveFilters(query);
        SqlParts parts = buildSql(reportType, f, query.detail());
        long total = count(parts, f, query);
        List<ReportRow> rows = fetch(parts, f, query, pageable);
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public ReportRow drillDown(ReportType reportType, UUID id, FinanceReportQuery query) {
        organizationService.requireUsable(query.organizationId());
        ResolvedFilters f = resolveFilters(query);
        SqlParts parts = buildDetailSql(reportType, f);
        List<ReportRow> rows = fetchById(parts, f, query, id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Registro do relatório", id);
        }
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public List<ReportRow> queryAll(ReportType reportType, FinanceReportQuery query, int maxRows) {
        organizationService.requireUsable(query.organizationId());
        ResolvedFilters f = resolveFilters(query);
        SqlParts parts = buildSql(reportType, f, query.detail());
        return fetch(parts, f, query, Pageable.ofSize(maxRows));
    }

    private ResolvedFilters resolveFilters(FinanceReportQuery query) {
        String tz = query.timezone() != null && !query.timezone().isBlank() ? query.timezone() : DEFAULT_TZ;
        LocalDate today = LocalDate.now(ZoneId.of(tz));
        LocalDate to = query.to() != null ? query.to() : today;
        LocalDate from = query.from() != null ? query.from() : to.withDayOfMonth(1);
        if (to.isBefore(from)) {
            throw new BusinessRuleException("Data final deve ser >= data inicial");
        }
        return new ResolvedFilters(
                query.organizationId(),
                query.storeId(),
                query.holderId(),
                query.categoryId(),
                query.costCenterId(),
                from,
                to,
                tz,
                today,
                blankToNull(query.status()),
                blankToNull(query.q()));
    }

    private SqlParts buildSql(ReportType type, ResolvedFilters f, boolean detail) {
        return switch (type) {
            case PAYABLES -> payablesSql(f, detail);
            case RECEIVABLES -> receivablesSql(f, detail);
            case DUE_DATES -> dueDatesSql(f, detail);
            case DELINQUENCY -> delinquencySql(f, detail);
            case PAYMENTS -> paymentsSql(f, detail);
            case RECEIPTS -> receiptsSql(f, detail);
            case ACCOUNT_STATEMENT -> accountStatementSql(f, detail);
            case CASH_FLOW -> cashFlowSql(f, detail);
            case FORECAST -> forecastSql(f, detail);
            case CATEGORIES -> categoriesSql(f, detail);
            case COST_CENTERS -> costCentersSql(f, detail);
            case SUPPLIERS -> suppliersSql(f, detail);
            case CUSTOMERS -> customersSql(f, detail);
            case CARDS -> cardsSql(f, detail);
            case RECONCILIATION -> reconciliationSql(f, detail);
            case TRANSFERS -> transfersSql(f, detail);
            case REVERSALS -> reversalsSql(f, detail);
            case ADVANCES -> advancesSql(f, detail);
            case RENEGOTIATIONS -> renegotiationsSql(f, detail);
            case INCOME_STATEMENT -> incomeStatementSql(f, detail);
            case STORE_POSITION -> storePositionSql(f, detail);
        };
    }

    private SqlParts buildDetailSql(ReportType type, ResolvedFilters f) {
        String base = switch (type) {
            case PAYABLES ->
                    """
                    SELECT p.id, p.issue_date, COALESCE(p.document_number, 'Conta a pagar'),
                      p.balance_amount, p.status::text, p.store_id, NULL::uuid, p.financial_category_id, '{}'
                    FROM payables p WHERE p.organization_id = :orgId AND p.id = :id
                    """;
            case RECEIVABLES ->
                    """
                    SELECT r.id, r.issue_date, COALESCE(r.document_number, 'Conta a receber'),
                      r.balance_amount, r.status::text, r.store_id, NULL::uuid, r.financial_category_id, '{}'
                    FROM receivables r WHERE r.organization_id = :orgId AND r.id = :id
                    """;
            case PAYMENTS ->
                    """
                    SELECT ps.id, ps.payment_date, COALESCE(ps.reference_code, 'Pagamento'),
                      ps.total_disbursed, ps.status::text, ps.store_id, ps.holder_id, NULL::uuid, '{}'
                    FROM payable_settlements ps WHERE ps.organization_id = :orgId AND ps.id = :id
                    """;
            case RECEIPTS ->
                    """
                    SELECT rs.id, rs.payment_date, COALESCE(rs.reference_code, 'Recebimento'),
                      rs.net_amount, rs.status::text, rs.store_id, rs.holder_id, NULL::uuid, '{}'
                    FROM receivable_settlements rs WHERE rs.organization_id = :orgId AND rs.id = :id
                    """;
            case ACCOUNT_STATEMENT ->
                    """
                    SELECT m.id, (m.occurred_at AT TIME ZONE :tz)::date, COALESCE(m.description, m.movement_type),
                      m.amount, m.movement_type::text, h.store_id, m.holder_id, NULL::uuid, '{}'
                    FROM financial_holder_movements m
                    JOIN financial_account_holders h ON h.id = m.holder_id
                    WHERE h.organization_id = :orgId AND m.id = :id
                    """;
            case TRANSFERS ->
                    """
                    SELECT t.id, t.transfer_date, COALESCE(t.notes, 'Transferência'),
                      t.amount, t.status::text, t.source_store_id, t.source_holder_id, NULL::uuid, '{}'
                    FROM financial_transfers t WHERE t.organization_id = :orgId AND t.id = :id
                    """;
            case REVERSALS ->
                    """
                    SELECT rv.id, (rv.created_at AT TIME ZONE :tz)::date, COALESCE(rv.reason, 'Estorno'),
                      0, rv.status::text, rv.store_id, NULL::uuid, NULL::uuid, '{}'
                    FROM financial_reversals rv WHERE rv.organization_id = :orgId AND rv.id = :id
                    """;
            case RENEGOTIATIONS ->
                    """
                    SELECT rn.id, rn.renegotiation_date, COALESCE(rn.notes, 'Renegociação'),
                      rn.new_total_amount, rn.status::text, rn.store_id, NULL::uuid, NULL::uuid, '{}'
                    FROM financial_renegotiations rn WHERE rn.organization_id = :orgId AND rn.id = :id
                    """;
            default -> throw new BusinessRuleException("Drill-down não suportado para o tipo: " + type);
        };
        return new SqlParts(base, "SELECT 1", "date DESC", false);
    }

    private SqlParts payablesSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return new SqlParts(
                    """
                    SELECT p.id, p.issue_date, COALESCE(p.document_number, sup.trade_name, sup.legal_name),
                      p.balance_amount, p.status::text, p.store_id, NULL::uuid, p.financial_category_id, '{}'
                    FROM payables p
                    JOIN suppliers sup ON sup.id = p.supplier_id
                    WHERE p.organization_id = :orgId AND p.active = TRUE
                    """,
                    """
                    SELECT COUNT(*) FROM payables p
                    JOIN suppliers sup ON sup.id = p.supplier_id
                    WHERE p.organization_id = :orgId AND p.active = TRUE
                    """,
                    "issue_date DESC",
                    false);
        }
        return new SqlParts(
                """
                SELECT sup.id, MAX(p.issue_date), COALESCE(sup.trade_name, sup.legal_name),
                  SUM(p.balance_amount), 'OPEN', MAX(p.store_id), NULL::uuid, NULL::uuid, '{}'
                FROM payables p
                JOIN suppliers sup ON sup.id = p.supplier_id
                WHERE p.organization_id = :orgId AND p.active = TRUE
                  AND p.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                GROUP BY sup.id, sup.trade_name, sup.legal_name
                """,
                """
                SELECT COUNT(DISTINCT sup.id) FROM payables p
                JOIN suppliers sup ON sup.id = p.supplier_id
                WHERE p.organization_id = :orgId AND p.active = TRUE
                  AND p.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                """,
                "3 DESC",
                false);
    }

    private SqlParts receivablesSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return new SqlParts(
                    """
                    SELECT r.id, r.issue_date, COALESCE(r.document_number, c.trade_name, c.name),
                      r.balance_amount, r.status::text, r.store_id, NULL::uuid, r.financial_category_id, '{}'
                    FROM receivables r
                    JOIN customers c ON c.id = r.customer_id
                    WHERE r.organization_id = :orgId AND r.active = TRUE
                    """,
                    """
                    SELECT COUNT(*) FROM receivables r
                    JOIN customers c ON c.id = r.customer_id
                    WHERE r.organization_id = :orgId AND r.active = TRUE
                    """,
                    "issue_date DESC",
                    false);
        }
        return new SqlParts(
                """
                SELECT c.id, MAX(r.issue_date), COALESCE(c.trade_name, c.name),
                  SUM(r.balance_amount), 'OPEN', MAX(r.store_id), NULL::uuid, NULL::uuid, '{}'
                FROM receivables r
                JOIN customers c ON c.id = r.customer_id
                WHERE r.organization_id = :orgId AND r.active = TRUE
                  AND r.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                GROUP BY c.id, c.trade_name, c.name
                """,
                """
                SELECT COUNT(DISTINCT c.id) FROM receivables r
                JOIN customers c ON c.id = r.customer_id
                WHERE r.organization_id = :orgId AND r.active = TRUE
                  AND r.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                """,
                "3 DESC",
                false);
    }

    private SqlParts dueDatesSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT i.id, i.due_date, COALESCE(p.document_number, 'Pagar'), i.balance_amount,
                  i.status::text, p.store_id, NULL::uuid, p.financial_category_id, '{"side":"PAYABLE"}'
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.due_date BETWEEN :from AND :to
                UNION ALL
                SELECT i.id, i.due_date, COALESCE(r.document_number, 'Receber'), i.balance_amount,
                  i.status::text, r.store_id, NULL::uuid, r.financial_category_id, '{"side":"RECEIVABLE"}'
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.due_date BETWEEN :from AND :to
                """,
                """
                SELECT (
                  SELECT COUNT(*) FROM payable_installments i
                  JOIN payables p ON p.id = i.payable_id
                  WHERE p.organization_id = :orgId AND i.active = TRUE AND i.due_date BETWEEN :from AND :to
                ) + (
                  SELECT COUNT(*) FROM receivable_installments i
                  JOIN receivables r ON r.id = i.receivable_id
                  WHERE r.organization_id = :orgId AND i.active = TRUE AND i.due_date BETWEEN :from AND :to
                )
                """,
                "date ASC",
                false);
    }

    private SqlParts delinquencySql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT i.id, i.due_date, COALESCE(r.document_number, c.trade_name, c.name),
                  i.balance_amount, i.status::text, r.store_id, NULL::uuid, r.financial_category_id, '{}'
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                JOIN customers c ON c.id = r.customer_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date < :overdueBefore
                """,
                """
                SELECT COUNT(*) FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date < :overdueBefore
                """,
                "date ASC",
                false);
    }

    private SqlParts paymentsSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT ps.id, ps.payment_date, COALESCE(ps.reference_code, 'Pagamento'),
                  ps.total_disbursed, ps.status::text, ps.store_id, ps.holder_id, NULL::uuid, '{}'
                FROM payable_settlements ps
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.payment_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM payable_settlements ps
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.payment_date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts receiptsSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT rs.id, rs.payment_date, COALESCE(rs.reference_code, 'Recebimento'),
                  rs.net_amount, rs.status::text, rs.store_id, rs.holder_id, NULL::uuid, '{}'
                FROM receivable_settlements rs
                WHERE rs.organization_id = :orgId AND rs.active = TRUE
                  AND rs.payment_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM receivable_settlements rs
                WHERE rs.organization_id = :orgId AND rs.active = TRUE
                  AND rs.payment_date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts accountStatementSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT m.id, (m.occurred_at AT TIME ZONE :tz)::date,
                  COALESCE(m.description, m.movement_type), m.amount, m.movement_type::text,
                  h.store_id, m.holder_id, NULL::uuid, '{}'
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId AND m.active = TRUE AND m.reversed = FALSE
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId AND m.active = TRUE AND m.reversed = FALSE
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts cashFlowSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return accountStatementSql(f, true);
        }
        return new SqlParts(
                """
                SELECT gen_random_uuid(), (m.occurred_at AT TIME ZONE :tz)::date, 'Fluxo diário',
                  SUM(CASE WHEN m.amount >= 0 THEN m.amount ELSE 0 END) - SUM(CASE WHEN m.amount < 0 THEN ABS(m.amount) ELSE 0 END),
                  'REALIZED', MAX(h.store_id), MAX(m.holder_id), NULL::uuid, '{}'
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId AND m.active = TRUE AND m.reversed = FALSE
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                GROUP BY (m.occurred_at AT TIME ZONE :tz)::date
                """,
                """
                SELECT COUNT(DISTINCT (m.occurred_at AT TIME ZONE :tz)::date)
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId AND m.active = TRUE AND m.reversed = FALSE
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """,
                "date ASC",
                false);
    }

    private SqlParts forecastSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT i.id, i.due_date, COALESCE(r.document_number, 'Receber'), i.balance_amount,
                  i.status::text, r.store_id, NULL::uuid, r.financial_category_id, '{"side":"IN"}'
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :from AND :to
                UNION ALL
                SELECT i.id, i.due_date, COALESCE(p.document_number, 'Pagar'), i.balance_amount,
                  i.status::text, p.store_id, NULL::uuid, p.financial_category_id, '{"side":"OUT"}'
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :from AND :to
                """,
                """
                SELECT (
                  SELECT COUNT(*) FROM receivable_installments i
                  JOIN receivables r ON r.id = i.receivable_id
                  WHERE r.organization_id = :orgId AND i.active = TRUE
                    AND i.due_date BETWEEN :from AND :to
                ) + (
                  SELECT COUNT(*) FROM payable_installments i
                  JOIN payables p ON p.id = i.payable_id
                  WHERE p.organization_id = :orgId AND i.active = TRUE
                    AND i.due_date BETWEEN :from AND :to
                )
                """,
                "date ASC",
                false);
    }

    private SqlParts categoriesSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return new SqlParts(
                    """
                    SELECT e.id, e.entry_date, e.reason, e.amount, e.status::text,
                      e.store_id, e.holder_id, e.financial_category_id, '{"entryType":"' || e.entry_type || '"}'
                    FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.status = 'CONFIRMED'
                      AND e.entry_date BETWEEN :from AND :to
                    """,
                    """
                    SELECT COUNT(*) FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.status = 'CONFIRMED'
                      AND e.entry_date BETWEEN :from AND :to
                    """,
                    "date DESC",
                    false);
        }
        return new SqlParts(
                """
                SELECT c.id, MAX(e.entry_date), COALESCE(c.name, 'Sem categoria'),
                  SUM(e.amount), 'CONFIRMED', MAX(e.store_id), MAX(e.holder_id), c.id, '{}'
                FROM financial_entries e
                LEFT JOIN financial_categories c ON c.id = e.financial_category_id
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_date BETWEEN :from AND :to
                GROUP BY c.id, c.name
                """,
                """
                SELECT COUNT(DISTINCT COALESCE(e.financial_category_id, '00000000-0000-0000-0000-000000000000'::uuid))
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_date BETWEEN :from AND :to
                """,
                "amount DESC",
                false);
    }

    private SqlParts costCentersSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return new SqlParts(
                    """
                    SELECT e.id, e.entry_date, e.reason, e.amount, e.status::text,
                      e.store_id, e.holder_id, e.financial_category_id, '{"costCenterId":"' || COALESCE(e.cost_center_id::text,'') || '"}'
                    FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.entry_date BETWEEN :from AND :to
                    """,
                    """
                    SELECT COUNT(*) FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.entry_date BETWEEN :from AND :to
                    """,
                    "date DESC",
                    false);
        }
        return new SqlParts(
                """
                SELECT cc.id, MAX(e.entry_date), COALESCE(cc.name, 'Sem centro'),
                  SUM(e.amount), 'CONFIRMED', MAX(e.store_id), MAX(e.holder_id), MAX(e.financial_category_id), '{}'
                FROM financial_entries e
                LEFT JOIN cost_centers cc ON cc.id = e.cost_center_id
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'CONFIRMED'
                  AND e.entry_date BETWEEN :from AND :to
                GROUP BY cc.id, cc.name
                """,
                """
                SELECT COUNT(DISTINCT COALESCE(e.cost_center_id, '00000000-0000-0000-0000-000000000000'::uuid))
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.entry_date BETWEEN :from AND :to
                """,
                "amount DESC",
                false);
    }

    private SqlParts suppliersSql(ResolvedFilters f, boolean detail) {
        return payablesSql(f, detail);
    }

    private SqlParts customersSql(ResolvedFilters f, boolean detail) {
        return receivablesSql(f, detail);
    }

    private SqlParts cardsSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT s.id, s.expected_date, CONCAT('Cartão ', t.authorization_code),
                  s.net_amount, s.status::text, t.store_id, NULL::uuid, NULL::uuid,
                  '{"installment":' || s.installment_number || '}'
                FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE
                  AND s.expected_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE
                  AND s.expected_date BETWEEN :from AND :to
                """,
                "date ASC",
                false);
    }

    private SqlParts reconciliationSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT bse.id, bse.entry_date, bse.description, bse.amount,
                  bse.reconciliation_status::text, h.store_id, bse.holder_id, NULL::uuid, '{}'
                FROM bank_statement_entries bse
                JOIN financial_account_holders h ON h.id = bse.holder_id
                WHERE h.organization_id = :orgId AND bse.active = TRUE
                  AND bse.entry_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM bank_statement_entries bse
                JOIN financial_account_holders h ON h.id = bse.holder_id
                WHERE h.organization_id = :orgId AND bse.active = TRUE
                  AND bse.entry_date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts transfersSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT t.id, t.transfer_date, COALESCE(t.notes, 'Transferência'), t.amount,
                  t.status::text, t.source_store_id, t.source_holder_id, NULL::uuid, '{}'
                FROM financial_transfers t
                WHERE t.organization_id = :orgId AND t.active = TRUE
                  AND t.transfer_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM financial_transfers t
                WHERE t.organization_id = :orgId AND t.active = TRUE
                  AND t.transfer_date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts reversalsSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT rv.id, (rv.created_at AT TIME ZONE :tz)::date, COALESCE(rv.reason, 'Estorno'),
                  0, rv.status::text, rv.store_id, NULL::uuid, NULL::uuid,
                  '{"sourceType":"' || rv.source_type || '"}'
                FROM financial_reversals rv
                WHERE rv.organization_id = :orgId AND rv.active = TRUE
                  AND (rv.created_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM financial_reversals rv
                WHERE rv.organization_id = :orgId AND rv.active = TRUE
                  AND (rv.created_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts advancesSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT ca.id, ca.advance_date, COALESCE(ca.document_number, c.trade_name, c.name),
                  ca.balance_amount, ca.status::text, ca.store_id, ca.holder_id, NULL::uuid, '{"side":"CUSTOMER"}'
                FROM customer_advances ca
                JOIN customers c ON c.id = ca.customer_id
                WHERE ca.organization_id = :orgId AND ca.active = TRUE
                  AND ca.advance_date BETWEEN :from AND :to
                UNION ALL
                SELECT sa.id, sa.advance_date, COALESCE(sa.document_number, s.trade_name, s.legal_name),
                  sa.balance_amount, sa.status::text, sa.store_id, sa.holder_id, NULL::uuid, '{"side":"SUPPLIER"}'
                FROM supplier_advances sa
                JOIN suppliers s ON s.id = sa.supplier_id
                WHERE sa.organization_id = :orgId AND sa.active = TRUE
                  AND sa.advance_date BETWEEN :from AND :to
                """,
                """
                SELECT (
                  SELECT COUNT(*) FROM customer_advances ca
                  WHERE ca.organization_id = :orgId AND ca.active = TRUE AND ca.advance_date BETWEEN :from AND :to
                ) + (
                  SELECT COUNT(*) FROM supplier_advances sa
                  WHERE sa.organization_id = :orgId AND sa.active = TRUE AND sa.advance_date BETWEEN :from AND :to
                )
                """,
                "date DESC",
                false);
    }

    private SqlParts renegotiationsSql(ResolvedFilters f, boolean detail) {
        return new SqlParts(
                """
                SELECT rn.id, rn.renegotiation_date, COALESCE(rn.notes, 'Renegociação'),
                  rn.new_total_amount, rn.status::text, rn.store_id, NULL::uuid, NULL::uuid,
                  '{"side":"' || rn.document_side || '"}'
                FROM financial_renegotiations rn
                WHERE rn.organization_id = :orgId AND rn.active = TRUE
                  AND rn.renegotiation_date BETWEEN :from AND :to
                """,
                """
                SELECT COUNT(*) FROM financial_renegotiations rn
                WHERE rn.organization_id = :orgId AND rn.active = TRUE
                  AND rn.renegotiation_date BETWEEN :from AND :to
                """,
                "date DESC",
                false);
    }

    private SqlParts incomeStatementSql(ResolvedFilters f, boolean detail) {
        return categoriesSql(f, detail);
    }

    private SqlParts storePositionSql(ResolvedFilters f, boolean detail) {
        if (detail) {
            return new SqlParts(
                    """
                    SELECT h.id, CURRENT_DATE, h.name,
                      COALESCE(SUM(CASE WHEN m.active = TRUE AND m.reversed = FALSE THEN m.amount ELSE 0 END), 0),
                      h.status::text, h.store_id, h.id, NULL::uuid, '{}'
                    FROM financial_account_holders h
                    LEFT JOIN financial_holder_movements m ON m.holder_id = h.id
                    WHERE h.organization_id = :orgId AND h.active = TRUE
                    GROUP BY h.id, h.name, h.status, h.store_id
                    """,
                    """
                    SELECT COUNT(*) FROM financial_account_holders h
                    WHERE h.organization_id = :orgId AND h.active = TRUE
                    """,
                    "amount DESC",
                    false);
        }
        return new SqlParts(
                """
                SELECT COALESCE(h.store_id, '00000000-0000-0000-0000-000000000000'::uuid), CURRENT_DATE,
                  COALESCE(st.name, 'Sem loja'),
                  COALESCE(SUM(CASE WHEN m.active = TRUE AND m.reversed = FALSE THEN m.amount ELSE 0 END), 0),
                  'ACTIVE', h.store_id, NULL::uuid, NULL::uuid, '{}'
                FROM financial_account_holders h
                LEFT JOIN financial_holder_movements m ON m.holder_id = h.id
                LEFT JOIN stores st ON st.id = h.store_id
                WHERE h.organization_id = :orgId AND h.active = TRUE
                GROUP BY h.store_id, st.name
                """,
                """
                SELECT COUNT(DISTINCT COALESCE(h.store_id, '00000000-0000-0000-0000-000000000000'::uuid))
                FROM financial_account_holders h
                WHERE h.organization_id = :orgId AND h.active = TRUE
                """,
                "amount DESC",
                false);
    }

    @SuppressWarnings("unchecked")
    private List<ReportRow> fetch(SqlParts parts, ResolvedFilters f, FinanceReportQuery query, Pageable pageable) {
        StringBuilder sql = new StringBuilder(parts.selectSql());
        appendCommonFilters(sql, parts, f, query);
        sql.append(" ORDER BY ").append(resolveOrderBy(query.sort(), parts.defaultOrder()));
        Query q = em.createNativeQuery(sql.toString());
        bindCommon(q, f, query, sql.toString());
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<ReportRow> rows = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            rows.add(mapRow(row));
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<ReportRow> fetchById(SqlParts parts, ResolvedFilters f, FinanceReportQuery query, UUID id) {
        Query q = em.createNativeQuery(parts.selectSql())
                .setParameter("orgId", f.organizationId())
                .setParameter("id", id);
        if (parts.selectSql().contains(":tz")) {
            q.setParameter("tz", f.timezone());
        }
        List<ReportRow> rows = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            rows.add(mapRow(row));
        }
        return rows;
    }

    private long count(SqlParts parts, ResolvedFilters f, FinanceReportQuery query) {
        StringBuilder sql = new StringBuilder(parts.countSql());
        appendCountFilters(sql, parts, f, query);
        Query q = em.createNativeQuery(sql.toString());
        bindCommon(q, f, query, sql.toString());
        return ((Number) q.getSingleResult()).longValue();
    }

    private void appendCommonFilters(StringBuilder sql, SqlParts parts, ResolvedFilters f, FinanceReportQuery query) {
        String s = sql.toString();
        if (s.contains(" payables ") || s.contains(" FROM payables")) {
            appendPayableFilters(sql, f);
        }
        if (s.contains(" receivables ") || s.contains(" FROM receivables")) {
            appendReceivableFilters(sql, f);
        }
        if (s.contains("financial_entries") || s.contains(" FROM financial_entries")) {
            appendEntryFilters(sql, f);
        }
        if (s.contains("payable_settlements") || s.contains(" FROM payable_settlements")) {
            appendSettlementFilters(sql, f, "ps");
        }
        if (s.contains("receivable_settlements") || s.contains(" FROM receivable_settlements")) {
            appendSettlementFilters(sql, f, "rs");
        }
        if (s.contains("financial_holder_movements") || s.contains("financial_account_holders")) {
            appendHolderFilters(sql, f);
        }
        if (s.contains("card_transactions")) {
            appendStoreFilter(sql, f, "t");
        }
        if (s.contains("financial_transfers")) {
            appendStoreFilter(sql, f, "t");
        }
        if (s.contains("financial_reversals")) {
            appendStoreFilter(sql, f, "rv");
        }
        if (s.contains("financial_renegotiations")) {
            appendStoreFilter(sql, f, "rn");
        }
        if (s.contains("customer_advances")) {
            appendStoreFilter(sql, f, "ca");
        }
        if (s.contains("supplier_advances")) {
            appendStoreFilter(sql, f, "sa");
        }
        if (s.contains("bank_statement_entries")) {
            appendHolderFilters(sql, f);
        }
        if (f.status() != null && !s.contains("status::text")) {
            if (s.contains(" payables ")) {
                sql.append(" AND p.status = :status");
            } else if (s.contains(" receivables ")) {
                sql.append(" AND r.status = :status");
            } else if (s.contains("payable_settlements")) {
                sql.append(" AND ps.status = :status");
            } else if (s.contains("receivable_settlements")) {
                sql.append(" AND rs.status = :status");
            }
        }
        if (f.q() != null) {
            if (s.contains("document_number")) {
                sql.append(" AND (COALESCE(p.document_number, '') ILIKE :q OR COALESCE(r.document_number, '') ILIKE :q"
                        + " OR COALESCE(ps.reference_code, '') ILIKE :q OR COALESCE(rs.reference_code, '') ILIKE :q"
                        + " OR COALESCE(e.reason, '') ILIKE :q)");
            }
        }
    }

    private void appendCountFilters(StringBuilder sql, SqlParts parts, ResolvedFilters f, FinanceReportQuery query) {
        appendCommonFilters(sql, parts, f, query);
    }

    private void appendStoreFilter(StringBuilder sql, ResolvedFilters f, String alias) {
        if (f.storeId() != null) {
            sql.append(" AND ").append(alias).append(".store_id = :storeId");
        }
    }

    private void appendHolderFilters(StringBuilder sql, ResolvedFilters f) {
        if (f.storeId() != null) {
            sql.append(" AND h.store_id = :storeId");
        }
        if (f.holderId() != null) {
            sql.append(" AND h.id = :holderId");
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
        if (f.holderId() != null) {
            sql.append(" AND e.holder_id = :holderId");
        }
        if (f.categoryId() != null) {
            sql.append(" AND e.financial_category_id = :categoryId");
        }
        if (f.costCenterId() != null) {
            sql.append(" AND e.cost_center_id = :costCenterId");
        }
    }

    private void appendSettlementFilters(StringBuilder sql, ResolvedFilters f, String alias) {
        appendStoreFilter(sql, f, alias);
        if (f.holderId() != null) {
            sql.append(" AND ").append(alias).append(".holder_id = :holderId");
        }
    }

    private void bindCommon(Query q, ResolvedFilters f, FinanceReportQuery query, String sql) {
        q.setParameter("orgId", f.organizationId());
        if (sql.contains(":from")) {
            q.setParameter("from", f.from());
        }
        if (sql.contains(":to")) {
            q.setParameter("to", f.to());
        }
        if (sql.contains(":tz")) {
            q.setParameter("tz", f.timezone());
        }
        if (sql.contains(":overdueBefore")) {
            q.setParameter("overdueBefore", f.today());
        }
        if (sql.contains(":storeId") && f.storeId() != null) {
            q.setParameter("storeId", f.storeId());
        }
        if (sql.contains(":holderId") && f.holderId() != null) {
            q.setParameter("holderId", f.holderId());
        }
        if (sql.contains(":categoryId") && f.categoryId() != null) {
            q.setParameter("categoryId", f.categoryId());
        }
        if (sql.contains(":costCenterId") && f.costCenterId() != null) {
            q.setParameter("costCenterId", f.costCenterId());
        }
        if (sql.contains(":status") && f.status() != null) {
            q.setParameter("status", f.status());
        }
        if (sql.contains(":q") && f.q() != null) {
            q.setParameter("q", "%" + f.q() + "%");
        }
    }

    private String resolveOrderBy(String sort, String defaultOrder) {
        if (sort == null || sort.isBlank()) {
            return translateOrder(defaultOrder);
        }
        return switch (sort.toLowerCase()) {
            case "date", "date_asc" -> "2 ASC";
            case "date_desc" -> "2 DESC";
            case "amount", "amount_desc" -> "4 DESC";
            case "amount_asc" -> "4 ASC";
            case "description" -> "3 ASC";
            default -> translateOrder(defaultOrder);
        };
    }

    private String translateOrder(String order) {
        return order
                .replace("date ASC", "2 ASC")
                .replace("date DESC", "2 DESC")
                .replace("amount DESC", "4 DESC")
                .replace("amount ASC", "4 ASC")
                .replace("description ASC", "3 ASC")
                .replace("issue_date DESC", "2 DESC");
    }

    private ReportRow mapRow(Object[] row) {
        LocalDate date = row[1] instanceof java.sql.Date d
                ? d.toLocalDate()
                : row[1] != null ? LocalDate.parse(row[1].toString()) : null;
        return new ReportRow(
                uuid(row[0]),
                date,
                str(row[2]),
                money(row[3]),
                str(row[4]),
                uuid(row[5]),
                uuid(row[6]),
                uuid(row[7]),
                parseExtra(row.length > 8 ? row[8] : null));
    }

    private Map<String, Object> parseExtra(Object value) {
        if (value == null) {
            return Map.of();
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "{}".equals(text)) {
            return Map.of();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("raw", text);
        return map;
    }

    private String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private UUID uuid(Object value) {
        return value != null ? UUID.fromString(value.toString()) : null;
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
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

    private record ResolvedFilters(
            UUID organizationId,
            UUID storeId,
            UUID holderId,
            UUID categoryId,
            UUID costCenterId,
            LocalDate from,
            LocalDate to,
            String timezone,
            LocalDate today,
            String status,
            String q) {}

    private record SqlParts(String selectSql, String countSql, String defaultOrder, boolean grouped) {}
}
