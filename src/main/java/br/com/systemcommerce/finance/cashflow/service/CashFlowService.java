package br.com.systemcommerce.finance.cashflow.service;

import br.com.systemcommerce.finance.cashflow.dto.CashFlowDtos.*;
import br.com.systemcommerce.finance.cashflow.entity.CashFlowScenario;
import br.com.systemcommerce.finance.cashflow.entity.FinanceReportExportAudit;
import br.com.systemcommerce.finance.cashflow.repository.CashFlowScenarioRepository;
import br.com.systemcommerce.finance.cashflow.repository.FinanceReportExportAuditRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashFlowService {

    private static final int DRILL_DOWN_SAMPLE_LIMIT = 50;

    private final CashFlowScenarioRepository scenarioRepository;
    private final FinanceReportExportAuditRepository exportAuditRepository;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public CashFlowResponse build(CashFlowQuery query) {
        organizationService.requireUsable(query.organizationId());
        String tz = resolveTimezone(query.timezone());
        Perspective perspective = query.perspective() != null ? query.perspective() : Perspective.CONSOLIDATED;
        ScenarioFactors factors = resolveScenarioFactors(query.organizationId(), query.scenarioId());

        BigDecimal opening = computeOpeningBalance(query, tz);
        Map<LocalDate, DayTotals> realizedByDay = perspective != Perspective.PROJECTED
                ? loadRealizedByDay(query, tz)
                : Map.of();
        Map<LocalDate, DayTotals> projectedByDay = perspective != Perspective.REALIZED
                ? loadProjectedByDay(query)
                : Map.of();

        List<DayBucket> days = buildDayBuckets(query.from(), query.to(), opening, realizedByDay, projectedByDay, factors);
        CashFlowIndicators indicators = buildIndicators(days);

        List<BreakdownItem> byHolder = perspective != Perspective.PROJECTED
                ? loadBreakdown(query, tz, "HOLDER")
                : List.of();
        List<BreakdownItem> byStore = loadBreakdown(query, tz, "STORE");
        List<BreakdownItem> byCategory = perspective != Perspective.REALIZED
                ? loadBreakdown(query, tz, "CATEGORY")
                : List.of();
        List<BreakdownItem> byCostCenter = perspective != Perspective.REALIZED
                ? loadBreakdown(query, tz, "COST_CENTER")
                : List.of();
        List<DrillDownItem> sample = buildDrillDownItems(query, tz, DRILL_DOWN_SAMPLE_LIMIT);

        return new CashFlowResponse(indicators, days, byHolder, byStore, byCategory, byCostCenter, sample);
    }

    @Transactional(readOnly = true)
    public List<DrillDownItem> drillDown(CashFlowQuery query, String timezone, int limit) {
        String tz = resolveTimezone(timezone != null ? timezone : query.timezone());
        return buildDrillDownItems(query, tz, limit);
    }

    @Transactional
    public ScenarioResponse createScenario(ScenarioCreateRequest request) {
        if (scenarioRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Já existe cenário com este código");
        }
        CashFlowScenario scenario = new CashFlowScenario();
        scenario.setOrganization(organizationService.requireUsable(request.organizationId()));
        scenario.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        scenario.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        scenario.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        scenario.setInflowFactor(normalizeFactor(request.inflowFactor()));
        scenario.setOutflowFactor(normalizeFactor(request.outflowFactor()));
        CashFlowScenario saved = scenarioRepository.save(scenario);
        domainAuditService.record(
                "FINANCE", "CashFlowScenario", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Cenário criado");
        return toScenario(saved);
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> listScenarios(UUID organizationId) {
        return scenarioRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId).stream()
                .map(this::toScenario)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(CashFlowQuery query) {
        CashFlowResponse response = build(query);
        StringBuilder sb = new StringBuilder();
        sb.append("date,openingBalance,inflows,outflows,dailyBalance,accumulatedBalance,projectedBalance,cashNeed,availability\n");
        for (DayBucket day : response.days()) {
            sb.append(day.date())
                    .append(',')
                    .append(day.openingBalance())
                    .append(',')
                    .append(day.inflows())
                    .append(',')
                    .append(day.outflows())
                    .append(',')
                    .append(day.dailyBalance())
                    .append(',')
                    .append(day.accumulatedBalance())
                    .append(',')
                    .append(day.projectedBalance())
                    .append(',')
                    .append(day.cashNeed())
                    .append(',')
                    .append(day.availability())
                    .append('\n');
        }
        recordExportAudit(query, "CASH_FLOW", response.days().size());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<DrillDownItem> buildDrillDownItems(CashFlowQuery query, String tz, int limit) {
        List<DrillDownItem> items = new ArrayList<>();
        String realizedSql = realizedDrillDownSql(query);
        Query realizedQ = em.createNativeQuery(realizedSql)
                .setParameter("orgId", query.organizationId())
                .setParameter("from", query.from())
                .setParameter("to", query.to())
                .setParameter("tz", tz);
        bindOptionalHolderFilters(realizedQ, query, realizedSql);
        realizedQ.setMaxResults(limit);
        items.addAll(mapDrillDownRows(realizedQ.getResultList(), "REALIZED"));
        int remaining = limit - items.size();
        if (remaining > 0) {
            items.addAll(mapDrillDownRows(
                    em.createNativeQuery(projectedDrillDownSql(query))
                            .setParameter("orgId", query.organizationId())
                            .setParameter("from", query.from())
                            .setParameter("to", query.to())
                            .setMaxResults(remaining)
                            .getResultList(),
                    "PROJECTED"));
        }
        return items;
    }

    private String realizedDrillDownSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT m.source_document_type, m.source_document_id,
                       (m.occurred_at AT TIME ZONE :tz)::date AS dt,
                       COALESCE(m.description, m.movement_type) AS descr,
                       m.amount,
                       CASE
                         WHEN m.movement_type IN ('RECEIPT','OPENING_BALANCE') THEN 'IN'
                         WHEN m.movement_type = 'PAYMENT' THEN 'OUT'
                         WHEN m.amount >= 0 THEN 'IN'
                         ELSE 'OUT'
                       END AS direction
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId
                  AND m.reversed = FALSE AND m.active = TRUE
                  AND m.movement_type NOT IN ('TRANSFER_IN','TRANSFER_OUT')
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """);
        appendOptionalFilters(sql, query, "h");
        sql.append(" ORDER BY dt, m.occurred_at");
        return sql.toString();
    }

    private String projectedDrillDownSql(CashFlowQuery query) {
        return """
                SELECT 'RECEIVABLE_INSTALLMENT', i.id, i.due_date, COALESCE(r.document_number, 'Parcela receber'), i.balance_amount, 'IN'
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :from AND :to
                UNION ALL
                SELECT 'PAYABLE_INSTALLMENT', i.id, i.due_date, COALESCE(p.document_number, 'Parcela pagar'), i.balance_amount, 'OUT'
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :from AND :to
                ORDER BY 3
                """;
    }

    @SuppressWarnings("unchecked")
    private List<DrillDownItem> mapDrillDownRows(List<?> rows, String defaultSourceType) {
        List<DrillDownItem> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            String sourceType = cols[0] != null ? cols[0].toString() : defaultSourceType;
            UUID sourceId = cols[1] != null ? UUID.fromString(cols[1].toString()) : null;
            LocalDate date = cols[2] instanceof java.sql.Date d ? d.toLocalDate() : LocalDate.parse(cols[2].toString());
            String description = cols[3] != null ? cols[3].toString() : "";
            BigDecimal amount = money(cols[4]);
            String direction = cols[5].toString();
            result.add(new DrillDownItem(sourceType, sourceId, date, description, amount.abs(), direction));
        }
        return result;
    }

    private BigDecimal computeOpeningBalance(CashFlowQuery query, String tz) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(
                  CASE WHEN m.movement_type IN ('TRANSFER_IN','TRANSFER_OUT') THEN 0 ELSE m.amount END
                ), 0)
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId
                  AND m.reversed = FALSE AND m.active = TRUE
                  AND (m.occurred_at AT TIME ZONE :tz)::date < :from
                """);
        appendOptionalFilters(sql, query, "h");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", query.organizationId())
                .setParameter("from", query.from())
                .setParameter("tz", tz);
        bindOptionalHolderFilters(q, query, sql.toString());
        return money(q.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private Map<LocalDate, DayTotals> loadRealizedByDay(CashFlowQuery query, String tz) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT (m.occurred_at AT TIME ZONE :tz)::date AS dt,
                  COALESCE(SUM(CASE
                    WHEN m.movement_type IN ('RECEIPT','OPENING_BALANCE') THEN ABS(m.amount)
                    WHEN m.movement_type IN ('ADJUSTMENT','REVERSAL') AND m.amount > 0 THEN m.amount
                    ELSE 0 END), 0) AS inflows,
                  COALESCE(SUM(CASE
                    WHEN m.movement_type = 'PAYMENT' THEN ABS(m.amount)
                    WHEN m.movement_type IN ('ADJUSTMENT','REVERSAL') AND m.amount < 0 THEN ABS(m.amount)
                    ELSE 0 END), 0) AS outflows
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId
                  AND m.reversed = FALSE AND m.active = TRUE
                  AND m.movement_type NOT IN ('TRANSFER_IN','TRANSFER_OUT')
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """);
        appendOptionalFilters(sql, query, "h");
        sql.append(" GROUP BY dt ORDER BY dt");
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", query.organizationId())
                .setParameter("from", query.from())
                .setParameter("to", query.to())
                .setParameter("tz", tz);
        bindOptionalHolderFilters(q, query, sql.toString());
        Map<LocalDate, DayTotals> map = new HashMap<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            map.put(date, new DayTotals(money(row[1]), money(row[2])));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<LocalDate, DayTotals> loadProjectedByDay(CashFlowQuery query) {
        Map<LocalDate, DayTotals> map = new HashMap<>();
        mergeProjected(map, projectedReceivablesSql(query), query);
        mergeProjected(map, projectedCardSchedulesSql(query), query);
        mergeProjected(map, projectedBillingSql(query), query);
        mergeProjectedOut(map, projectedPayablesSql(query), query);
        mergeProjectedOut(map, projectedEntriesSql(query), query);
        return map;
    }

    private void mergeProjected(Map<LocalDate, DayTotals> map, String sql, CashFlowQuery query) {
        Query q = em.createNativeQuery(sql).setParameter("orgId", query.organizationId());
        bindOptionalProjectedParams(q, query);
        q.setParameter("from", query.from());
        q.setParameter("to", query.to());
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal inflow = money(row[1]);
            DayTotals current = map.getOrDefault(date, DayTotals.ZERO);
            map.put(date, new DayTotals(current.inflows().add(inflow), current.outflows()));
        }
    }

    private void mergeProjectedOut(Map<LocalDate, DayTotals> map, String sql, CashFlowQuery query) {
        Query q = em.createNativeQuery(sql).setParameter("orgId", query.organizationId());
        bindOptionalProjectedParams(q, query);
        q.setParameter("from", query.from());
        q.setParameter("to", query.to());
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal outflow = money(row[1]);
            DayTotals current = map.getOrDefault(date, DayTotals.ZERO);
            map.put(date, new DayTotals(current.inflows(), current.outflows().add(outflow)));
        }
    }

    private String projectedReceivablesSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT i.due_date, COALESCE(SUM(i.balance_amount), 0)
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :from AND :to
                """);
        if (query.storeId() != null) {
            sql.append(" AND r.store_id = :storeId");
        }
        if (query.categoryId() != null) {
            sql.append(" AND r.financial_category_id = :categoryId");
        }
        if (query.costCenterId() != null) {
            sql.append(" AND r.cost_center_id = :costCenterId");
        }
        sql.append(" GROUP BY i.due_date");
        return sql.toString();
    }

    private String projectedCardSchedulesSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT s.expected_date, COALESCE(SUM(s.net_amount), 0)
                FROM card_receivable_schedules s
                JOIN card_transactions t ON t.id = s.card_transaction_id
                WHERE t.organization_id = :orgId AND s.active = TRUE
                  AND s.status = 'SCHEDULED'
                  AND s.expected_date BETWEEN :from AND :to
                """);
        if (query.storeId() != null) {
            sql.append(" AND t.store_id = :storeId");
        }
        sql.append(" GROUP BY s.expected_date");
        return sql.toString();
    }

    private String projectedBillingSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT b.due_date, COALESCE(SUM(b.amount), 0)
                FROM billing_documents b
                WHERE b.organization_id = :orgId AND b.active = TRUE
                  AND b.status = 'PENDING'
                  AND b.billing_type IN ('PIX','BOLETO')
                  AND b.receivable_installment_id IS NULL
                  AND b.due_date BETWEEN :from AND :to
                """);
        if (query.storeId() != null) {
            sql.append(" AND b.store_id = :storeId");
        }
        sql.append(" GROUP BY b.due_date");
        return sql.toString();
    }

    private String projectedPayablesSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT i.due_date, COALESCE(SUM(i.balance_amount), 0)
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :from AND :to
                """);
        if (query.storeId() != null) {
            sql.append(" AND p.store_id = :storeId");
        }
        if (query.categoryId() != null) {
            sql.append(" AND p.financial_category_id = :categoryId");
        }
        if (query.costCenterId() != null) {
            sql.append(" AND p.cost_center_id = :costCenterId");
        }
        sql.append(" GROUP BY i.due_date");
        return sql.toString();
    }

    private String projectedEntriesSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT e.entry_date, COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.status = 'DRAFT'
                  AND e.entry_type IN ('MANUAL_EXPENSE','TAX')
                  AND e.entry_date BETWEEN :from AND :to
                """);
        if (query.storeId() != null) {
            sql.append(" AND e.store_id = :storeId");
        }
        if (query.holderId() != null) {
            sql.append(" AND e.holder_id = :holderId");
        }
        if (query.categoryId() != null) {
            sql.append(" AND e.financial_category_id = :categoryId");
        }
        if (query.costCenterId() != null) {
            sql.append(" AND e.cost_center_id = :costCenterId");
        }
        sql.append(" GROUP BY e.entry_date");
        return sql.toString();
    }

    private void bindOptionalProjectedParams(Query q, CashFlowQuery query) {
        if (query.storeId() != null) {
            q.setParameter("storeId", query.storeId());
        }
        if (query.holderId() != null) {
            q.setParameter("holderId", query.holderId());
        }
        if (query.categoryId() != null) {
            q.setParameter("categoryId", query.categoryId());
        }
        if (query.costCenterId() != null) {
            q.setParameter("costCenterId", query.costCenterId());
        }
    }

    @SuppressWarnings("unchecked")
    private List<BreakdownItem> loadBreakdown(CashFlowQuery query, String tz, String dimension) {
        String sql = switch (dimension) {
            case "HOLDER" -> breakdownHolderSql(query);
            case "STORE" -> breakdownStoreSql(query, tz);
            case "CATEGORY" -> breakdownCategorySql(query);
            case "COST_CENTER" -> breakdownCostCenterSql(query);
            default -> throw new IllegalArgumentException("Dimensão inválida: " + dimension);
        };
        Query q = em.createNativeQuery(sql)
                .setParameter("orgId", query.organizationId())
                .setParameter("from", query.from())
                .setParameter("to", query.to());
        if (sql.contains(":tz")) {
            q.setParameter("tz", tz);
        }
        bindOptionalProjectedParams(q, query);
        bindOptionalHolderFilters(q, query, sql);
        List<BreakdownItem> items = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            items.add(new BreakdownItem(
                    dimension,
                    row[0] != null ? row[0].toString() : "NONE",
                    row[1] != null ? row[1].toString() : "—",
                    money(row[2]),
                    money(row[3])));
        }
        return items;
    }

    private String breakdownHolderSql(CashFlowQuery query) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT h.id::text, h.name,
                  COALESCE(SUM(CASE
                    WHEN m.movement_type IN ('RECEIPT','OPENING_BALANCE') THEN ABS(m.amount)
                    WHEN m.movement_type IN ('ADJUSTMENT','REVERSAL') AND m.amount > 0 THEN m.amount ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN m.movement_type = 'PAYMENT' THEN ABS(m.amount)
                    WHEN m.movement_type IN ('ADJUSTMENT','REVERSAL') AND m.amount < 0 THEN ABS(m.amount) ELSE 0 END), 0)
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                WHERE h.organization_id = :orgId AND m.reversed = FALSE AND m.active = TRUE
                  AND m.movement_type NOT IN ('TRANSFER_IN','TRANSFER_OUT')
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                """);
        appendOptionalFilters(sql, query, "h");
        sql.append(" GROUP BY h.id, h.name ORDER BY h.name");
        return sql.toString();
    }

    private String breakdownStoreSql(CashFlowQuery query, String tz) {
        return """
                SELECT COALESCE(h.store_id::text, 'NONE'), COALESCE(s.name, 'Sem loja'),
                  COALESCE(SUM(CASE WHEN m.amount > 0 THEN m.amount ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN m.amount < 0 THEN ABS(m.amount) ELSE 0 END), 0)
                FROM financial_holder_movements m
                JOIN financial_account_holders h ON h.id = m.holder_id
                LEFT JOIN stores s ON s.id = h.store_id
                WHERE h.organization_id = :orgId AND m.reversed = FALSE AND m.active = TRUE
                  AND m.movement_type NOT IN ('TRANSFER_IN','TRANSFER_OUT')
                  AND (m.occurred_at AT TIME ZONE :tz)::date BETWEEN :from AND :to
                GROUP BY h.store_id, s.name ORDER BY s.name NULLS LAST
                """;
    }

    private String breakdownCategorySql(CashFlowQuery query) {
        return """
                SELECT COALESCE(r.financial_category_id::text, 'NONE'), COALESCE(c.name, 'Sem categoria'),
                  COALESCE(SUM(i.balance_amount), 0), 0
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                LEFT JOIN financial_categories c ON c.id = r.financial_category_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :from AND :to
                GROUP BY r.financial_category_id, c.name
                UNION ALL
                SELECT COALESCE(p.financial_category_id::text, 'NONE'), COALESCE(c.name, 'Sem categoria'),
                  0, COALESCE(SUM(i.balance_amount), 0)
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                LEFT JOIN financial_categories c ON c.id = p.financial_category_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :from AND :to
                GROUP BY p.financial_category_id, c.name
                """;
    }

    private String breakdownCostCenterSql(CashFlowQuery query) {
        return """
                SELECT COALESCE(r.cost_center_id::text, 'NONE'), COALESCE(cc.name, 'Sem centro'),
                  COALESCE(SUM(i.balance_amount), 0), 0
                FROM receivable_installments i
                JOIN receivables r ON r.id = i.receivable_id
                LEFT JOIN cost_centers cc ON cc.id = r.cost_center_id
                WHERE r.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                  AND i.due_date BETWEEN :from AND :to
                GROUP BY r.cost_center_id, cc.name
                UNION ALL
                SELECT COALESCE(p.cost_center_id::text, 'NONE'), COALESCE(cc.name, 'Sem centro'),
                  0, COALESCE(SUM(i.balance_amount), 0)
                FROM payable_installments i
                JOIN payables p ON p.id = i.payable_id
                LEFT JOIN cost_centers cc ON cc.id = p.cost_center_id
                WHERE p.organization_id = :orgId AND i.active = TRUE
                  AND i.status IN ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                  AND i.due_date BETWEEN :from AND :to
                GROUP BY p.cost_center_id, cc.name
                """;
    }

    private void appendOptionalFilters(StringBuilder sql, CashFlowQuery query, String holderAlias) {
        if (query.storeId() != null) {
            sql.append(" AND ").append(holderAlias).append(".store_id = :storeId");
        }
        if (query.holderId() != null) {
            sql.append(" AND ").append(holderAlias).append(".id = :holderId");
        }
    }

    private void bindOptionalHolderFilters(Query q, CashFlowQuery query, String sql) {
        if (sql.contains(":storeId") && query.storeId() != null) {
            q.setParameter("storeId", query.storeId());
        }
        if (sql.contains(":holderId") && query.holderId() != null) {
            q.setParameter("holderId", query.holderId());
        }
    }

    private List<DayBucket> buildDayBuckets(
            LocalDate from,
            LocalDate to,
            BigDecimal opening,
            Map<LocalDate, DayTotals> realized,
            Map<LocalDate, DayTotals> projected,
            ScenarioFactors factors) {
        List<DayBucket> days = new ArrayList<>();
        BigDecimal accumulated = opening;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayTotals r = realized.getOrDefault(cursor, DayTotals.ZERO);
            DayTotals p = projected.getOrDefault(cursor, DayTotals.ZERO);
            BigDecimal inflows = scale(r.inflows().add(p.inflows()).multiply(factors.inflowFactor()));
            BigDecimal outflows = scale(r.outflows().add(p.outflows()).multiply(factors.outflowFactor()));
            BigDecimal daily = inflows.subtract(outflows);
            accumulated = accumulated.add(daily);
            BigDecimal projectedBalance = accumulated;
            BigDecimal cashNeed = projectedBalance.compareTo(BigDecimal.ZERO) < 0
                    ? projectedBalance.abs()
                    : BigDecimal.ZERO;
            BigDecimal availability = projectedBalance.max(BigDecimal.ZERO);
            days.add(new DayBucket(
                    cursor,
                    cursor.equals(from) ? opening : null,
                    inflows,
                    outflows,
                    daily,
                    accumulated,
                    projectedBalance,
                    cashNeed,
                    availability));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private CashFlowIndicators buildIndicators(List<DayBucket> days) {
        if (days.isEmpty()) {
            return new CashFlowIndicators(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }
        BigDecimal opening = days.getFirst().openingBalance() != null ? days.getFirst().openingBalance() : BigDecimal.ZERO;
        BigDecimal totalIn = days.stream().map(DayBucket::inflows).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = days.stream().map(DayBucket::outflows).reduce(BigDecimal.ZERO, BigDecimal::add);
        DayBucket last = days.getLast();
        return new CashFlowIndicators(opening, totalIn, totalOut, last.projectedBalance(), last.cashNeed(), last.availability());
    }

    private ScenarioFactors resolveScenarioFactors(UUID organizationId, UUID scenarioId) {
        if (scenarioId == null) {
            return ScenarioFactors.DEFAULT;
        }
        CashFlowScenario scenario = scenarioRepository
                .findByIdAndOrganizationId(scenarioId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cenário de fluxo de caixa", scenarioId));
        return new ScenarioFactors(normalizeFactor(scenario.getInflowFactor()), normalizeFactor(scenario.getOutflowFactor()));
    }

    private void recordExportAudit(CashFlowQuery query, String reportType, int rowCount) {
        FinanceReportExportAudit audit = new FinanceReportExportAudit();
        audit.setOrganizationId(query.organizationId());
        audit.setStoreId(query.storeId());
        CurrentUser.id().ifPresent(audit::setUserId);
        audit.setReportType(reportType);
        audit.setExportFormat("CSV");
        audit.setRowCount(rowCount);
        try {
            audit.setFiltersJson(objectMapper.writeValueAsString(query));
        } catch (JsonProcessingException ignored) {
            audit.setFiltersJson(null);
        }
        exportAuditRepository.save(audit);
    }

    private ScenarioResponse toScenario(CashFlowScenario scenario) {
        return new ScenarioResponse(
                scenario.getId(),
                scenario.getOrganization().getId(),
                scenario.getCode(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getInflowFactor(),
                scenario.getOutflowFactor());
    }

    private String resolveTimezone(String timezone) {
        return timezone != null && !timezone.isBlank() ? timezone : "America/Sao_Paulo";
    }

    private BigDecimal normalizeFactor(BigDecimal factor) {
        return factor != null && factor.compareTo(BigDecimal.ZERO) > 0 ? factor : BigDecimal.ONE;
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

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record DayTotals(BigDecimal inflows, BigDecimal outflows) {
        static final DayTotals ZERO = new DayTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private record ScenarioFactors(BigDecimal inflowFactor, BigDecimal outflowFactor) {
        static final ScenarioFactors DEFAULT = new ScenarioFactors(BigDecimal.ONE, BigDecimal.ONE);
    }
}
