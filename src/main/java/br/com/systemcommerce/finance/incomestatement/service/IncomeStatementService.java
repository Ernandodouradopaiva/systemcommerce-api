package br.com.systemcommerce.finance.incomestatement.service;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.account.repository.FinancialCategoryRepository;
import br.com.systemcommerce.finance.cashflow.entity.FinanceReportExportAudit;
import br.com.systemcommerce.finance.cashflow.repository.FinanceReportExportAuditRepository;
import br.com.systemcommerce.finance.incomestatement.dto.IncomeStatementDtos.*;
import br.com.systemcommerce.finance.incomestatement.entity.*;
import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementExecution.Basis;
import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementLine.LineType;
import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementMapping.SourceType;
import br.com.systemcommerce.finance.incomestatement.repository.*;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncomeStatementService {

    private static final String DEFAULT_LAYOUT_CODE = "DEFAULT";
    private static final Pattern FORMULA_TOKEN = Pattern.compile("([+-]?)([A-Z0-9_]+)");

    private final IncomeStatementLayoutRepository layoutRepository;
    private final IncomeStatementLineRepository lineRepository;
    private final IncomeStatementMappingRepository mappingRepository;
    private final IncomeStatementExecutionRepository executionRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final FinanceReportExportAuditRepository exportAuditRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public LayoutResponse ensureDefaultLayout(UUID organizationId) {
        organizationService.requireUsable(organizationId);
        Optional<IncomeStatementLayout> existing =
                layoutRepository.findByOrganizationIdAndCodeIgnoreCase(organizationId, DEFAULT_LAYOUT_CODE);
        if (existing.isPresent()) {
            return toLayout(existing.get());
        }
        IncomeStatementLayout layout = new IncomeStatementLayout();
        layout.setOrganization(organizationService.requireUsable(organizationId));
        layout.setCode(DEFAULT_LAYOUT_CODE);
        layout.setName("DRE gerencial padrão");
        layout.setDescription("Layout gerencial padrão — não constitui demonstração contábil oficial.");

        List<DefaultLineDef> defs = defaultLineDefinitions();
        Map<String, IncomeStatementLine> byCode = new LinkedHashMap<>();
        for (DefaultLineDef def : defs) {
            IncomeStatementLine line = new IncomeStatementLine();
            line.setLayout(layout);
            line.setCode(def.code());
            line.setName(def.name());
            line.setLineType(def.lineType());
            line.setSortOrder(def.sortOrder());
            line.setFormula(def.formula());
            line.setFormulaDoc(def.formulaDoc());
            line.setSignMultiplier(def.signMultiplier());
            layout.getLines().add(line);
            byCode.put(def.code(), line);
        }
        seedDefaultMappings(layout, byCode);
        IncomeStatementLayout saved = layoutRepository.save(layout);
        domainAuditService.record(
                "FINANCE",
                "IncomeStatementLayout",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Layout DRE gerencial padrão criado");
        return toLayout(saved);
    }

    @Transactional(readOnly = true)
    public List<LayoutResponse> listLayouts(UUID organizationId) {
        return layoutRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId).stream()
                .map(this::toLayoutSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public LayoutResponse getLayout(UUID layoutId) {
        IncomeStatementLayout layout = layoutRepository
                .findDetailedById(layoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Layout DRE gerencial", layoutId));
        return toLayout(layout);
    }

    @Transactional
    public MappingResponse mapCategory(UUID layoutId, MappingCreateRequest request) {
        IncomeStatementLayout layout = requireLayout(layoutId);
        IncomeStatementLine line = lineRepository
                .findById(request.lineId())
                .orElseThrow(() -> new ResourceNotFoundException("Linha DRE", request.lineId()));
        if (!line.getLayout().getId().equals(layoutId)) {
            throw new BusinessRuleException("Linha não pertence ao layout informado");
        }
        IncomeStatementMapping mapping = new IncomeStatementMapping();
        mapping.setLayout(layout);
        mapping.setLine(line);
        mapping.setSourceType(request.sourceType());
        if (request.financialCategoryId() != null) {
            FinancialCategory category = categoryRepository
                    .findById(request.financialCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria financeira", request.financialCategoryId()));
            mapping.setFinancialCategory(category);
        }
        IncomeStatementMapping saved = mappingRepository.save(mapping);
        domainAuditService.record(
                "FINANCE",
                "IncomeStatementMapping",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Mapeamento DRE criado");
        return toMapping(saved);
    }

    @Transactional
    public ExecutionResponse execute(ExecuteRequest request) {
        if (request.to().isBefore(request.from())) {
            throw new BusinessRuleException("Data final deve ser >= data inicial");
        }
        IncomeStatementLayout layout = requireLayout(request.layoutId());
        IncomeStatementExecution execution = new IncomeStatementExecution();
        execution.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            execution.setStore(storeService.requireUsable(request.storeId()));
        }
        execution.setLayout(layout);
        execution.setBasis(request.basis());
        execution.setPeriodFrom(request.from());
        execution.setPeriodTo(request.to());
        execution.setCompareFrom(request.compareFrom());
        execution.setCompareTo(request.compareTo());
        execution.setTimezone(resolveTimezone(request.timezone()));
        execution.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        execution.setExecutedAt(Instant.now());
        CurrentUser.id().ifPresent(execution::setExecutedBy);

        List<IncomeStatementLine> lines =
                lineRepository.findByLayoutIdAndActiveTrueOrderBySortOrderAsc(layout.getId());
        List<IncomeStatementMapping> mappings = mappingRepository.findByLayoutIdAndActiveTrue(layout.getId());

        Map<String, BigDecimal> amounts = computeLineAmounts(lines, mappings, request.basis(), request.organizationId(), request.storeId(), request.from(), request.to());
        Map<String, BigDecimal> compareAmounts = Map.of();
        if (request.compareFrom() != null && request.compareTo() != null) {
            compareAmounts = computeLineAmounts(
                    lines, mappings, request.basis(), request.organizationId(), request.storeId(), request.compareFrom(), request.compareTo());
        }

        for (IncomeStatementLine line : lines) {
            BigDecimal amount = amounts.getOrDefault(line.getCode(), BigDecimal.ZERO);
            BigDecimal compare = compareAmounts.getOrDefault(line.getCode(), null);
            IncomeStatementExecutionLine execLine = new IncomeStatementExecutionLine();
            execLine.setExecution(execution);
            execLine.setLine(line);
            execLine.setLineCode(line.getCode());
            execLine.setLineName(line.getName());
            execLine.setAmount(scale(amount));
            execLine.setCompareAmount(compare != null ? scale(compare) : null);
            if (compare != null) {
                execLine.setVarianceAmount(scale(amount.subtract(compare)));
            }
            if (line.getLineType() == LineType.FORMULA) {
                execLine.setFormulaApplied(line.getFormula());
            }
            execLine.setSortOrder(line.getSortOrder());
            execution.getLines().add(execLine);
        }

        String cogsNote = "CMV baseado em categorias mapeadas; custo unitário de estoque ainda não integrado.";
        if (execution.getNotes() == null) {
            execution.setNotes(cogsNote);
        } else if (!execution.getNotes().contains("CMV")) {
            execution.setNotes(execution.getNotes() + " " + cogsNote);
        }

        IncomeStatementExecution saved = executionRepository.save(execution);
        domainAuditService.record(
                "FINANCE",
                "IncomeStatementExecution",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "DRE gerencial executada");
        return toExecution(executionRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(UUID executionId) {
        IncomeStatementExecution execution = executionRepository
                .findDetailedById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execução DRE gerencial", executionId));
        return toExecution(execution);
    }

    @Transactional(readOnly = true)
    public List<DrillDownItem> drillDown(DrillDownQuery query) {
        IncomeStatementExecution execution = executionRepository
                .findDetailedById(query.executionId())
                .orElseThrow(() -> new ResourceNotFoundException("Execução DRE gerencial", query.executionId()));
        IncomeStatementLine line = lineRepository.findByLayoutIdAndActiveTrueOrderBySortOrderAsc(
                        execution.getLayout().getId())
                .stream()
                .filter(l -> l.getCode().equalsIgnoreCase(query.lineCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Linha DRE", query.lineCode()));
        int limit = query.limit() != null && query.limit() > 0 ? query.limit() : 100;
        return loadDrillDown(execution, line, limit);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID executionId) {
        ExecutionResponse execution = getExecution(executionId);
        StringBuilder sb = new StringBuilder();
        sb.append("lineCode,lineName,amount,compareAmount,varianceAmount,formulaApplied\n");
        for (ExecutionLineResponse line : execution.lines()) {
            sb.append(csv(line.lineCode()))
                    .append(',')
                    .append(csv(line.lineName()))
                    .append(',')
                    .append(line.amount())
                    .append(',')
                    .append(line.compareAmount() != null ? line.compareAmount() : "")
                    .append(',')
                    .append(line.varianceAmount() != null ? line.varianceAmount() : "")
                    .append(',')
                    .append(csv(line.formulaApplied()))
                    .append('\n');
        }
        recordExportAudit(execution, execution.lines().size());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, BigDecimal> computeLineAmounts(
            List<IncomeStatementLine> lines,
            List<IncomeStatementMapping> mappings,
            Basis basis,
            UUID organizationId,
            UUID storeId,
            LocalDate from,
            LocalDate to) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (IncomeStatementLine line : lines) {
            if (line.getLineType() == LineType.HEADER) {
                amounts.put(line.getCode(), BigDecimal.ZERO);
                continue;
            }
            if (line.getLineType() == LineType.FORMULA || line.getLineType() == LineType.TOTAL) {
                continue;
            }
            BigDecimal value = computeDetailLine(line, mappings, basis, organizationId, storeId, from, to);
            int sign = line.getSignMultiplier() != null ? line.getSignMultiplier() : 1;
            amounts.put(line.getCode(), scale(value.multiply(BigDecimal.valueOf(sign))));
        }
        for (IncomeStatementLine line : lines) {
            if (line.getLineType() == LineType.FORMULA || line.getLineType() == LineType.TOTAL) {
                BigDecimal computed = evaluateFormula(line.getFormula(), amounts);
                int sign = line.getSignMultiplier() != null ? line.getSignMultiplier() : 1;
                amounts.put(line.getCode(), scale(computed.multiply(BigDecimal.valueOf(sign))));
            }
        }
        return amounts;
    }

    private BigDecimal computeDetailLine(
            IncomeStatementLine line,
            List<IncomeStatementMapping> mappings,
            Basis basis,
            UUID organizationId,
            UUID storeId,
            LocalDate from,
            LocalDate to) {
        List<IncomeStatementMapping> lineMappings = mappings.stream()
                .filter(m -> m.getLine().getId().equals(line.getId()))
                .toList();
        BigDecimal total = BigDecimal.ZERO;
        for (IncomeStatementMapping mapping : lineMappings) {
            total = total.add(switch (mapping.getSourceType()) {
                case SALES_GROSS -> sumSalesGross(organizationId, storeId, from, to, false);
                case SALES_DISCOUNT -> sumSalesDiscount(organizationId, storeId, from, to);
                case SALES_CANCELLED -> sumSalesGross(organizationId, storeId, from, to, true);
                case COGS_MAPPED -> sumCategoryCompetenceOrCash(
                        basis, organizationId, storeId, mapping.getFinancialCategory(), from, to, true);
                case CATEGORY -> sumCategoryCompetenceOrCash(
                        basis, organizationId, storeId, mapping.getFinancialCategory(), from, to, false);
                case ACCOUNT -> BigDecimal.ZERO;
            });
        }
        if (lineMappings.isEmpty() && "RECEITA_BRUTA".equals(line.getCode())) {
            total = sumSalesGross(organizationId, storeId, from, to, false);
        }
        return total;
    }

    private BigDecimal sumSalesGross(UUID orgId, UUID storeId, LocalDate from, LocalDate to, boolean cancelled) {
        StringBuilder sql = new StringBuilder(
                cancelled
                        ? """
                        SELECT COALESCE(SUM(s.total_amount), 0) FROM sales s
                        WHERE s.organization_id = :orgId AND s.active = TRUE AND s.status = 'CANCELLED'
                          AND (s.sale_date AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
                        """
                        : """
                        SELECT COALESCE(SUM(s.total_amount), 0) FROM sales s
                        WHERE s.organization_id = :orgId AND s.active = TRUE
                          AND s.status IN ('CONFIRMED','PAID','PARTIALLY_PAID')
                          AND (s.sale_date AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
                        """);
        if (storeId != null) {
            sql.append(" AND s.store_id = :storeId");
        }
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", orgId)
                .setParameter("from", from)
                .setParameter("to", to);
        if (storeId != null) {
            q.setParameter("storeId", storeId);
        }
        return money(q.getSingleResult());
    }

    private BigDecimal sumSalesDiscount(UUID orgId, UUID storeId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COALESCE(SUM(s.discount_amount), 0) FROM sales s
                WHERE s.organization_id = :orgId AND s.active = TRUE
                  AND s.status IN ('CONFIRMED','PAID','PARTIALLY_PAID')
                  AND (s.sale_date AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
                """);
        if (storeId != null) {
            sql.append(" AND s.store_id = :storeId");
        }
        Query q = em.createNativeQuery(sql.toString())
                .setParameter("orgId", orgId)
                .setParameter("from", from)
                .setParameter("to", to);
        if (storeId != null) {
            q.setParameter("storeId", storeId);
        }
        return money(q.getSingleResult());
    }

    private BigDecimal sumCategoryCompetenceOrCash(
            Basis basis,
            UUID orgId,
            UUID storeId,
            FinancialCategory category,
            LocalDate from,
            LocalDate to,
            boolean expenseOnly) {
        if (category == null) {
            return BigDecimal.ZERO;
        }
        if (basis == Basis.COMPETENCE) {
            return sumCompetenceByCategory(orgId, storeId, category.getId(), from, to, expenseOnly);
        }
        return sumCashByCategory(orgId, storeId, category.getId(), from, to, expenseOnly);
    }

    private BigDecimal sumCompetenceByCategory(
            UUID orgId, UUID storeId, UUID categoryId, LocalDate from, LocalDate to, boolean expenseOnly) {
        BigDecimal receivables = expenseOnly
                ? BigDecimal.ZERO
                : nativeSum(
                        """
                        SELECT COALESCE(SUM(r.total_amount - r.received_amount), 0)
                        FROM receivables r
                        WHERE r.organization_id = :orgId AND r.active = TRUE
                          AND r.financial_category_id = :catId
                          AND r.competence_date BETWEEN :from AND :to
                          AND r.status NOT IN ('DRAFT','CANCELLED')
                        """,
                        orgId,
                        storeId,
                        categoryId,
                        from,
                        to);
        BigDecimal payables = nativeSum(
                """
                SELECT COALESCE(SUM(p.total_amount - p.paid_amount), 0)
                FROM payables p
                WHERE p.organization_id = :orgId AND p.active = TRUE
                  AND p.financial_category_id = :catId
                  AND p.competence_date BETWEEN :from AND :to
                  AND p.status NOT IN ('DRAFT','CANCELLED')
                """,
                orgId,
                storeId,
                categoryId,
                from,
                to);
        BigDecimal entries = nativeSum(
                """
                SELECT COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.financial_category_id = :catId
                  AND e.competence_date BETWEEN :from AND :to
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type NOT IN ('OPENING_BALANCE')
                """,
                orgId,
                storeId,
                categoryId,
                from,
                to);
        return expenseOnly ? payables.add(entries) : receivables.add(entries);
    }

    private BigDecimal sumCashByCategory(
            UUID orgId, UUID storeId, UUID categoryId, LocalDate from, LocalDate to, boolean expenseOnly) {
        BigDecimal receivableSettlements = expenseOnly
                ? BigDecimal.ZERO
                : nativeSum(
                        """
                        SELECT COALESCE(SUM(rs.net_amount), 0)
                        FROM receivable_settlements rs
                        JOIN receivable_settlement_allocations rsa ON rsa.settlement_id = rs.id
                        JOIN receivable_installments ri ON ri.id = rsa.installment_id
                        JOIN receivables r ON r.id = ri.receivable_id
                        WHERE rs.organization_id = :orgId AND rs.active = TRUE
                          AND rs.status = 'CONFIRMED'
                          AND r.financial_category_id = :catId
                          AND rs.payment_date BETWEEN :from AND :to
                        """,
                        orgId,
                        storeId,
                        categoryId,
                        from,
                        to);
        BigDecimal payableSettlements = nativeSum(
                """
                SELECT COALESCE(SUM(ps.total_disbursed), 0)
                FROM payable_settlements ps
                JOIN payable_settlement_allocations psa ON psa.settlement_id = ps.id
                JOIN payable_installments pi ON pi.id = psa.installment_id
                JOIN payables p ON p.id = pi.payable_id
                WHERE ps.organization_id = :orgId AND ps.active = TRUE
                  AND ps.status = 'CONFIRMED'
                  AND p.financial_category_id = :catId
                  AND ps.payment_date BETWEEN :from AND :to
                """,
                orgId,
                storeId,
                categoryId,
                from,
                to);
        BigDecimal entries = nativeSum(
                """
                SELECT COALESCE(SUM(e.amount), 0)
                FROM financial_entries e
                WHERE e.organization_id = :orgId AND e.active = TRUE
                  AND e.financial_category_id = :catId
                  AND e.entry_date BETWEEN :from AND :to
                  AND e.status = 'CONFIRMED'
                  AND e.entry_type NOT IN ('OPENING_BALANCE')
                """,
                orgId,
                storeId,
                categoryId,
                from,
                to);
        return expenseOnly ? payableSettlements.add(entries) : receivableSettlements.add(entries);
    }

    private BigDecimal nativeSum(
            String sql, UUID orgId, UUID storeId, UUID categoryId, LocalDate from, LocalDate to) {
        if (storeId != null && !sql.contains(":storeId")) {
            sql = sql.replace("WHERE ", "WHERE 1=1 AND ");
        }
        StringBuilder sb = new StringBuilder(sql);
        if (storeId != null) {
            if (sql.contains("receivable_settlements")) {
                sb.append(" AND rs.store_id = :storeId");
            } else if (sql.contains("payable_settlements")) {
                sb.append(" AND ps.store_id = :storeId");
            } else if (sql.contains(" receivables ")) {
                sb.append(" AND r.store_id = :storeId");
            } else if (sql.contains(" payables ")) {
                sb.append(" AND p.store_id = :storeId");
            } else if (sql.contains("financial_entries")) {
                sb.append(" AND e.store_id = :storeId");
            }
        }
        Query q = em.createNativeQuery(sb.toString())
                .setParameter("orgId", orgId)
                .setParameter("catId", categoryId)
                .setParameter("from", from)
                .setParameter("to", to);
        if (storeId != null && sb.toString().contains(":storeId")) {
            q.setParameter("storeId", storeId);
        }
        return money(q.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<DrillDownItem> loadDrillDown(IncomeStatementExecution execution, IncomeStatementLine line, int limit) {
        UUID orgId = execution.getOrganization().getId();
        UUID storeId = execution.getStore() != null ? execution.getStore().getId() : null;
        LocalDate from = execution.getPeriodFrom();
        LocalDate to = execution.getPeriodTo();
        Basis basis = execution.getBasis();

        List<IncomeStatementMapping> mappings =
                mappingRepository.findByLineIdAndActiveTrue(line.getId());
        List<DrillDownItem> items = new ArrayList<>();

        if ("RECEITA_BRUTA".equals(line.getCode()) || mappings.stream().anyMatch(m -> m.getSourceType() == SourceType.SALES_GROSS)) {
            items.addAll(mapDrillRows(
                    em.createNativeQuery(
                                    """
                                    SELECT 'SALE', s.id, (s.sale_date AT TIME ZONE 'UTC')::date,
                                           s.sale_number, s.total_amount
                                    FROM sales s
                                    WHERE s.organization_id = :orgId AND s.active = TRUE
                                      AND s.status IN ('CONFIRMED','PAID','PARTIALLY_PAID')
                                      AND (s.sale_date AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
                                    ORDER BY s.sale_date LIMIT :lim
                                    """)
                            .setParameter("orgId", orgId)
                            .setParameter("from", from)
                            .setParameter("to", to)
                            .setParameter("lim", limit)
                            .getResultList()));
        }

        for (IncomeStatementMapping mapping : mappings) {
            if (mapping.getFinancialCategory() == null) {
                continue;
            }
            UUID catId = mapping.getFinancialCategory().getId();
            String sql = basis == Basis.COMPETENCE
                    ? """
                    SELECT 'FINANCIAL_ENTRY', e.id, e.competence_date, e.reason, e.amount
                    FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.financial_category_id = :catId
                      AND e.competence_date BETWEEN :from AND :to
                      AND e.status = 'CONFIRMED'
                    ORDER BY e.competence_date LIMIT :lim
                    """
                    : """
                    SELECT 'FINANCIAL_ENTRY', e.id, e.entry_date, e.reason, e.amount
                    FROM financial_entries e
                    WHERE e.organization_id = :orgId AND e.active = TRUE
                      AND e.financial_category_id = :catId
                      AND e.entry_date BETWEEN :from AND :to
                      AND e.status = 'CONFIRMED'
                    ORDER BY e.entry_date LIMIT :lim
                    """;
            items.addAll(mapDrillRows(em.createNativeQuery(sql)
                    .setParameter("orgId", orgId)
                    .setParameter("catId", catId)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("lim", limit)
                    .getResultList()));
        }
        return items.stream().limit(limit).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DrillDownItem> mapDrillRows(List<?> rows) {
        List<DrillDownItem> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            LocalDate date = cols[2] instanceof java.sql.Date d ? d.toLocalDate() : LocalDate.parse(cols[2].toString());
            result.add(new DrillDownItem(
                    cols[0].toString(),
                    cols[1] != null ? UUID.fromString(cols[1].toString()) : null,
                    date,
                    cols[3] != null ? cols[3].toString() : "",
                    money(cols[4])));
        }
        return result;
    }

    private BigDecimal evaluateFormula(String formula, Map<String, BigDecimal> amounts) {
        if (formula == null || formula.isBlank()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        Matcher matcher = FORMULA_TOKEN.matcher(formula.replace(" ", ""));
        while (matcher.find()) {
            String sign = matcher.group(1);
            String code = matcher.group(2);
            BigDecimal value = amounts.getOrDefault(code, BigDecimal.ZERO);
            if ("-".equals(sign)) {
                total = total.subtract(value);
            } else {
                total = total.add(value);
            }
        }
        return total;
    }

    private void seedDefaultMappings(IncomeStatementLayout layout, Map<String, IncomeStatementLine> byCode) {
        addMapping(layout, byCode.get("RECEITA_BRUTA"), SourceType.SALES_GROSS, null);
        addMapping(layout, byCode.get("DESCONTOS"), SourceType.SALES_DISCOUNT, null);
        addMapping(layout, byCode.get("CMV"), SourceType.COGS_MAPPED, null);
    }

    private void addMapping(
            IncomeStatementLayout layout, IncomeStatementLine line, SourceType sourceType, FinancialCategory category) {
        IncomeStatementMapping mapping = new IncomeStatementMapping();
        mapping.setLayout(layout);
        mapping.setLine(line);
        mapping.setSourceType(sourceType);
        mapping.setFinancialCategory(category);
        layout.getMappings().add(mapping);
    }

    private List<DefaultLineDef> defaultLineDefinitions() {
        return List.of(
                new DefaultLineDef("RECEITA_BRUTA", "Receita bruta", LineType.DETAIL, 10, null,
                        "Soma das vendas confirmadas/pagas no período (regime informado).", 1),
                new DefaultLineDef("DESCONTOS", "Descontos concedidos", LineType.DETAIL, 20, null,
                        "Descontos comerciais sobre vendas.", -1),
                new DefaultLineDef("IMPOSTOS_VENDAS", "Impostos sobre vendas", LineType.DETAIL, 30, null,
                        "Impostos mapeados por categoria financeira.", -1),
                new DefaultLineDef("RECEITA_LIQUIDA", "Receita líquida", LineType.FORMULA, 40,
                        "RECEITA_BRUTA-DESCONTOS-IMPOSTOS_VENDAS",
                        "Receita bruta menos descontos e impostos.", 1),
                new DefaultLineDef("CMV", "Custo das mercadorias vendidas", LineType.DETAIL, 50, null,
                        "CMV por categorias mapeadas; sem custo unitário de estoque nesta versão.", -1),
                new DefaultLineDef("MARGEM_BRUTA", "Margem bruta", LineType.FORMULA, 60, "RECEITA_LIQUIDA-CMV",
                        "Receita líquida menos CMV.", 1),
                new DefaultLineDef("DESP_COMERCIAIS", "Despesas comerciais", LineType.DETAIL, 70, null,
                        "Despesas comerciais por categoria.", -1),
                new DefaultLineDef("DESP_ADMIN", "Despesas administrativas", LineType.DETAIL, 80, null,
                        "Despesas administrativas por categoria.", -1),
                new DefaultLineDef("DESP_FINANCEIRAS", "Despesas financeiras", LineType.DETAIL, 90, null,
                        "Despesas financeiras por categoria.", -1),
                new DefaultLineDef("RESULTADO_OPERACIONAL", "Resultado operacional", LineType.FORMULA, 100,
                        "MARGEM_BRUTA-DESP_COMERCIAIS-DESP_ADMIN-DESP_FINANCEIRAS",
                        "Margem bruta menos despesas operacionais.", 1),
                new DefaultLineDef("OUTRAS", "Outras receitas/despesas", LineType.DETAIL, 110, null,
                        "Itens não classificados nas linhas anteriores.", 1),
                new DefaultLineDef("RESULTADO_GERENCIAL", "Resultado gerencial", LineType.FORMULA, 120,
                        "RESULTADO_OPERACIONAL+OUTRAS",
                        "Resultado gerencial — não substitui demonstração contábil oficial.", 1));
    }

    private IncomeStatementLayout requireLayout(UUID layoutId) {
        return layoutRepository
                .findById(layoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Layout DRE gerencial", layoutId));
    }

    private LayoutResponse toLayoutSummary(IncomeStatementLayout layout) {
        return new LayoutResponse(
                layout.getId(),
                layout.getOrganization().getId(),
                layout.getCode(),
                layout.getName(),
                layout.getDescription(),
                List.of());
    }

    private LayoutResponse toLayout(IncomeStatementLayout layout) {
        List<LineResponse> lines = layout.getLines().stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive()))
                .sorted(Comparator.comparing(IncomeStatementLine::getSortOrder))
                .map(this::toLine)
                .toList();
        return new LayoutResponse(
                layout.getId(),
                layout.getOrganization().getId(),
                layout.getCode(),
                layout.getName(),
                layout.getDescription(),
                lines);
    }

    private LineResponse toLine(IncomeStatementLine line) {
        return new LineResponse(
                line.getId(),
                line.getCode(),
                line.getName(),
                line.getLineType().name(),
                line.getSortOrder(),
                line.getFormula(),
                line.getFormulaDoc(),
                line.getSignMultiplier());
    }

    private MappingResponse toMapping(IncomeStatementMapping mapping) {
        return new MappingResponse(
                mapping.getId(),
                mapping.getLine().getId(),
                mapping.getFinancialCategory() != null ? mapping.getFinancialCategory().getId() : null,
                mapping.getFinancialAccount() != null ? mapping.getFinancialAccount().getId() : null,
                mapping.getSourceType().name());
    }

    private ExecutionResponse toExecution(IncomeStatementExecution execution) {
        List<ExecutionLineResponse> lines = execution.getLines().stream()
                .sorted(Comparator.comparing(IncomeStatementExecutionLine::getSortOrder))
                .map(l -> new ExecutionLineResponse(
                        l.getLine().getId(),
                        l.getLineCode(),
                        l.getLineName(),
                        l.getLine().getLineType().name(),
                        l.getAmount(),
                        l.getCompareAmount(),
                        l.getVarianceAmount(),
                        l.getFormulaApplied(),
                        l.getSortOrder()))
                .toList();
        return new ExecutionResponse(
                execution.getId(),
                execution.getOrganization().getId(),
                execution.getStore() != null ? execution.getStore().getId() : null,
                execution.getLayout().getId(),
                execution.getBasis().name(),
                execution.getPeriodFrom(),
                execution.getPeriodTo(),
                execution.getCompareFrom(),
                execution.getCompareTo(),
                execution.getTimezone(),
                execution.getExecutedAt(),
                execution.getExecutedBy(),
                execution.getNotes(),
                lines);
    }

    private void recordExportAudit(ExecutionResponse execution, int rowCount) {
        FinanceReportExportAudit audit = new FinanceReportExportAudit();
        audit.setOrganizationId(execution.organizationId());
        audit.setStoreId(execution.storeId());
        CurrentUser.id().ifPresent(audit::setUserId);
        audit.setReportType("INCOME_STATEMENT");
        audit.setExportFormat("CSV");
        audit.setRowCount(rowCount);
        try {
            audit.setFiltersJson(objectMapper.writeValueAsString(Map.of("executionId", execution.id())));
        } catch (JsonProcessingException ignored) {
            audit.setFiltersJson(null);
        }
        exportAuditRepository.save(audit);
    }

    private String resolveTimezone(String timezone) {
        return timezone != null && !timezone.isBlank() ? timezone : "America/Sao_Paulo";
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

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private record DefaultLineDef(
            String code,
            String name,
            LineType lineType,
            int sortOrder,
            String formula,
            String formulaDoc,
            int signMultiplier) {}
}
