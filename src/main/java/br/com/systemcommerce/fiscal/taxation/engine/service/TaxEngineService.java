package br.com.systemcommerce.fiscal.taxation.engine.service;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationChannel;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import br.com.systemcommerce.fiscal.taxation.engine.ConditionOperator;
import br.com.systemcommerce.fiscal.taxation.engine.TaxKind;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationItemRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationItemResponse;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationResponse;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationTraceResponse;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculationItem;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculationTrace;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRule;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleCondition;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRuleResult;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxCalculationItemRepository;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxCalculationRepository;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxCalculationTraceRepository;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxRuleRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TaxEngineService {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final TaxRuleRepository taxRuleRepository;
    private final TaxCalculationRepository calculationRepository;
    private final TaxCalculationItemRepository calculationItemRepository;
    private final TaxCalculationTraceRepository traceRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TaxCalculationResponse simulate(TaxCalculationRequest request) {
        return calculate(request, true);
    }

    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        return calculate(request, request.simulation() == null || request.simulation());
    }

    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request, boolean simulation) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Store store = storeService.requireUsable(request.storeId());
        if (!store.getOrganization().getId().equals(organization.getId())) {
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                    "Loja não pertence à organização informada");
        }

        FiscalEstablishment establishment = null;
        if (request.establishmentId() != null) {
            establishment = establishmentRepository
                    .findById(request.establishmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", request.establishmentId()));
        }

        LocalDate issuedOn = request.issuedOn() != null ? request.issuedOn() : LocalDate.now();
        List<TaxRule> rules = taxRuleRepository.findActiveRulesForDate(organization.getId(), issuedOn);

        TaxCalculation calculation = new TaxCalculation();
        calculation.setOrganization(organization);
        calculation.setStore(store);
        calculation.setEstablishment(establishment);
        calculation.setSimulation(simulation);
        calculation.setOriginDocumentType(request.originDocumentType());
        calculation.setOriginDocumentId(request.originDocumentId());
        calculation.setOperationCode(request.operationCode());
        calculation.setIssuedOn(issuedOn);
        calculation.setStatus(TaxCalculation.CalculationStatus.COMPLETED);
        calculation = calculationRepository.save(calculation);

        CalculationContext ctx = buildContext(request, issuedOn);
        List<TaxCalculationTrace> traces = new ArrayList<>();
        int stepOrder = 0;

        BigDecimal totalProducts = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        int lineNumber = 1;

        for (TaxCalculationItemRequest itemReq : request.items()) {
            TaxCalculationItem item = new TaxCalculationItem();
            item.setCalculation(calculation);
            item.setLineNumber(lineNumber++);
            item.setProductId(itemReq.productId());
            item.setNcm(itemReq.ncm());
            item.setCest(itemReq.cest());
            item.setOriginCode(itemReq.originCode());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(roundMoney(itemReq.unitPrice()));
            BigDecimal lineTotal = roundMoney(itemReq.quantity().multiply(itemReq.unitPrice()));
            item.setTotalAmount(lineTotal);
            totalProducts = totalProducts.add(lineTotal);

            Map<String, Object> breakdown = new LinkedHashMap<>();
            List<String> matchedCodes = new ArrayList<>();
            BigDecimal itemTax = BigDecimal.ZERO;

            boolean anyMatched = false;
            for (TaxRule rule : rules) {
                if (matchesRule(rule, ctx, itemReq)) {
                    anyMatched = true;
                    matchedCodes.add(rule.getCode());
                    applyRuleResults(rule, breakdown, lineTotal);
                    BigDecimal ruleTax = extractTaxAmount(breakdown, lineTotal);
                    itemTax = itemTax.add(ruleTax);

                    TaxCalculationTrace trace = new TaxCalculationTrace();
                    trace.setCalculation(calculation);
                    trace.setItem(item);
                    trace.setStepOrder(++stepOrder);
                    trace.setMessage("Regra aplicada: " + rule.getCode());
                    trace.setRule(rule);
                    trace.setDetailJson(toJson(Map.of("taxKind", rule.getTaxKind(), "code", rule.getCode())));
                    traces.add(trace);
                }
            }

            if (!anyMatched) {
                TaxCalculationTrace trace = new TaxCalculationTrace();
                trace.setCalculation(calculation);
                trace.setItem(item);
                trace.setStepOrder(++stepOrder);
                trace.setMessage("Nenhuma regra correspondente para o item");
                traces.add(trace);
            }

            item.setTaxBreakdownJson(toJson(breakdown));
            item.setSelectedRuleCodes(String.join(",", matchedCodes));
            calculationItemRepository.save(item);
            totalTax = totalTax.add(itemTax);
        }

        calculation.setTotalProducts(roundMoney(totalProducts));
        calculation.setTotalTax(roundMoney(totalTax));
        calculation.setTraceSummary(
                rules.isEmpty() ? "Nenhuma regra ativa; impostos zerados" : "Cálculo concluído com " + rules.size() + " regras candidatas");
        calculationRepository.save(calculation);

        for (TaxCalculationTrace trace : traces) {
            traceRepository.save(trace);
        }

        domainAuditService.record(
                "FISCAL",
                "TaxCalculation",
                calculation.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("simulation", simulation, "totalTax", totalTax),
                simulation ? "Simulação tributária" : "Cálculo tributário");

        return getById(calculation.getId());
    }

    @Transactional(readOnly = true)
    public TaxCalculationResponse getById(UUID id) {
        TaxCalculation calculation = calculationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cálculo tributário", id));

        List<TaxCalculationItemResponse> items =
                calculationItemRepository.findByCalculationIdOrderByLineNumber(id).stream()
                        .map(i -> new TaxCalculationItemResponse(
                                i.getId(),
                                i.getLineNumber(),
                                i.getProductId(),
                                i.getNcm(),
                                i.getCest(),
                                i.getOriginCode(),
                                i.getQuantity(),
                                i.getUnitPrice(),
                                i.getTotalAmount(),
                                i.getTaxBreakdownJson(),
                                i.getSelectedRuleCodes()))
                        .toList();

        List<TaxCalculationTraceResponse> traces =
                traceRepository.findByCalculationIdOrderByStepOrder(id).stream()
                        .map(t -> new TaxCalculationTraceResponse(
                                t.getId(),
                                t.getStepOrder(),
                                t.getMessage(),
                                t.getRule() != null ? t.getRule().getId() : null,
                                t.getItem() != null ? t.getItem().getId() : null,
                                t.getDetailJson()))
                        .toList();

        return new TaxCalculationResponse(
                calculation.getId(),
                calculation.getOrganization().getId(),
                calculation.getStore().getId(),
                calculation.getEstablishment() != null ? calculation.getEstablishment().getId() : null,
                Boolean.TRUE.equals(calculation.getSimulation()),
                calculation.getOperationCode(),
                calculation.getIssuedOn(),
                calculation.getStatus(),
                calculation.getTotalProducts(),
                calculation.getTotalTax(),
                calculation.getCurrency(),
                calculation.getTraceSummary(),
                items,
                traces,
                calculation.getCreatedAt());
    }

    CalculationContext buildContext(TaxCalculationRequest request, LocalDate issuedOn) {
        String originUf = normalizeUf(request.originUf());
        String destUf = normalizeUf(request.destinationUf());
        boolean sameState = originUf != null && originUf.equals(destUf);
        boolean interstate = originUf != null && destUf != null && !sameState;
        return new CalculationContext(
                issuedOn,
                request.operationCode(),
                request.channel() != null ? request.channel() : CalculationChannel.ERP,
                originUf,
                destUf,
                request.destinationIbge(),
                request.purpose() != null ? request.purpose() : CalculationPurpose.SALE,
                Boolean.TRUE.equals(request.finalConsumer()),
                request.taxpayerIndicator(),
                sameState,
                interstate);
    }

    boolean matchesRule(TaxRule rule, CalculationContext ctx, TaxCalculationItemRequest item) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            return true;
        }
        List<TaxRuleCondition> conditions = rule.getConditions().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .toList();
        for (TaxRuleCondition condition : conditions) {
            if (!evaluateCondition(condition, ctx, item)) {
                return false;
            }
        }
        return true;
    }

    boolean evaluateCondition(TaxRuleCondition condition, CalculationContext ctx, TaxCalculationItemRequest item) {
        String field = condition.getFieldName();
        String actual = resolveFieldValue(field, ctx, item);
        ConditionOperator op = condition.getOperator();
        String expected = condition.getValueText();

        return switch (op) {
            case EQ -> expected != null && expected.equalsIgnoreCase(actual);
            case NE -> expected == null || !expected.equalsIgnoreCase(actual);
            case IN -> expected != null
                    && actual != null
                    && List.of(expected.split(",")).stream()
                            .map(String::trim)
                            .anyMatch(v -> v.equalsIgnoreCase(actual));
            case IS_NULL -> actual == null || actual.isBlank();
            case MATCH -> expected != null && actual != null && actual.matches(expected);
        };
    }

    String resolveFieldValue(String field, CalculationContext ctx, TaxCalculationItemRequest item) {
        return switch (field) {
            case "originUf" -> ctx.originUf();
            case "destinationUf", "destUf" -> ctx.destinationUf();
            case "sameState" -> String.valueOf(ctx.sameState());
            case "interstate" -> String.valueOf(ctx.interstate());
            case "finalConsumer" -> String.valueOf(ctx.finalConsumer());
            case "taxpayerIndicator" ->
                    ctx.taxpayerIndicator() != null ? ctx.taxpayerIndicator().name() : null;
            case "purpose" -> ctx.purpose().name();
            case "channel" -> ctx.channel().name();
            case "ncmPrefix" -> item.ncm() != null && item.ncm().length() >= 4 ? item.ncm().substring(0, 4) : item.ncm();
            case "ncm" -> item.ncm();
            default -> null;
        };
    }

    void applyRuleResults(TaxRule rule, Map<String, Object> breakdown, BigDecimal lineTotal) {
        if (rule.getResults() == null) {
            return;
        }
        Map<String, Object> taxEntry = new LinkedHashMap<>();
        taxEntry.put("taxKind", rule.getTaxKind().name());
        taxEntry.put("ruleCode", rule.getCode());

        for (TaxRuleResult result : rule.getResults()) {
            if ("rate".equalsIgnoreCase(result.getResultKey()) && result.getNumericValue() != null) {
                BigDecimal rate = result.getNumericValue().setScale(RATE_SCALE, ROUNDING);
                BigDecimal taxAmount = roundMoney(lineTotal.multiply(rate).divide(BigDecimal.valueOf(100), RATE_SCALE, ROUNDING));
                taxEntry.put("rate", rate);
                taxEntry.put("amount", taxAmount);
            } else if (result.getResultKey() != null) {
                taxEntry.put(
                        result.getResultKey(),
                        result.getNumericValue() != null ? result.getNumericValue() : result.getResultValue());
            }
        }
        breakdown.put(rule.getTaxKind().name(), taxEntry);
    }

    BigDecimal extractTaxAmount(Map<String, Object> breakdown, BigDecimal lineTotal) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Object value : breakdown.values()) {
            if (value instanceof Map<?, ?> map && map.get("amount") instanceof BigDecimal amount) {
                sum = sum.add(amount);
            }
        }
        return sum;
    }

    private BigDecimal roundMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    private String normalizeUf(String uf) {
        return StringUtils.hasText(uf) ? uf.trim().toUpperCase() : null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public record CalculationContext(
            LocalDate issuedOn,
            String operationCode,
            CalculationChannel channel,
            String originUf,
            String destinationUf,
            String destinationIbge,
            CalculationPurpose purpose,
            boolean finalConsumer,
            TaxpayerIndicator taxpayerIndicator,
            boolean sameState,
            boolean interstate) {}
}
