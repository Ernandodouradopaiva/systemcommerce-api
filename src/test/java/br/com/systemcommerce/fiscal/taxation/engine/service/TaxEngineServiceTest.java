package br.com.systemcommerce.fiscal.taxation.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationChannel;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import br.com.systemcommerce.fiscal.taxation.engine.ConditionOperator;
import br.com.systemcommerce.fiscal.taxation.engine.TaxKind;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationItemRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationResponse;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
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
import br.com.systemcommerce.shared.audit.DomainAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxEngineServiceTest {

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @Mock
    private TaxCalculationRepository calculationRepository;

    @Mock
    private TaxCalculationItemRepository calculationItemRepository;

    @Mock
    private TaxCalculationTraceRepository traceRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaxEngineService taxEngineService;

    @Test
    void shouldApplyIcmsRuleForCeToCeFinalConsumer() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        LocalDate issuedOn = LocalDate.of(2026, 1, 15);

        Organization organization = new Organization();
        organization.setId(orgId);
        Store store = new Store();
        store.setId(storeId);
        store.setOrganization(organization);

        TaxRule rule = buildCeFinalConsumerRule();
        TaxCalculation calculation = new TaxCalculation();
        calculation.setId(UUID.randomUUID());
        calculation.setOrganization(organization);
        calculation.setStore(store);
        calculation.setSimulation(true);
        calculation.setIssuedOn(issuedOn);
        calculation.setStatus(TaxCalculation.CalculationStatus.COMPLETED);
        calculation.setTotalProducts(new BigDecimal("100.00"));
        calculation.setTotalTax(new BigDecimal("18.00"));
        calculation.setCurrency("BRL");

        when(organizationService.requireUsable(orgId)).thenReturn(organization);
        when(storeService.requireUsable(storeId)).thenReturn(store);
        when(taxRuleRepository.findActiveRulesForDate(eq(orgId), eq(issuedOn))).thenReturn(List.of(rule));
        when(calculationRepository.save(any(TaxCalculation.class))).thenAnswer(inv -> {
            TaxCalculation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });
        when(calculationItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(traceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(calculationRepository.findDetailedById(any())).thenReturn(Optional.of(calculation));
        when(calculationItemRepository.findByCalculationIdOrderByLineNumber(any())).thenReturn(List.of());
        when(traceRepository.findByCalculationIdOrderByStepOrder(any())).thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        TaxCalculationRequest request = new TaxCalculationRequest(
                orgId,
                storeId,
                null,
                true,
                issuedOn,
                "VENDA",
                CalculationChannel.POS,
                "CE",
                "CE",
                null,
                CalculationPurpose.SALE,
                true,
                null,
                null,
                null,
                List.of(new TaxCalculationItemRequest(
                        null, "12345678", null, "0", new BigDecimal("1"), new BigDecimal("100.00"), "UN")));

        TaxCalculationResponse response = taxEngineService.simulate(request);

        assertThat(response).isNotNull();
        assertThat(taxEngineService.matchesRule(rule, taxEngineService.buildContext(request, issuedOn), request.items().get(0)))
                .isTrue();
    }

    private TaxRule buildCeFinalConsumerRule() {
        TaxRule rule = new TaxRule();
        rule.setId(UUID.randomUUID());
        rule.setCode("ICMS-CE-FC");
        rule.setTaxKind(TaxKind.ICMS);
        rule.setPriority(100);
        rule.setValidFrom(LocalDate.of(2020, 1, 1));
        rule.setActive(true);
        rule.setStatus(TaxRule.RuleStatus.ACTIVE);

        List<TaxRuleCondition> conditions = new ArrayList<>();
        TaxRuleCondition origin = new TaxRuleCondition();
        origin.setFieldName("originUf");
        origin.setOperator(ConditionOperator.EQ);
        origin.setValueText("CE");
        origin.setSortOrder(0);
        conditions.add(origin);

        TaxRuleCondition dest = new TaxRuleCondition();
        dest.setFieldName("destinationUf");
        dest.setOperator(ConditionOperator.EQ);
        dest.setValueText("CE");
        dest.setSortOrder(1);
        conditions.add(dest);

        TaxRuleCondition fc = new TaxRuleCondition();
        fc.setFieldName("finalConsumer");
        fc.setOperator(ConditionOperator.EQ);
        fc.setValueText("true");
        fc.setSortOrder(2);
        conditions.add(fc);

        rule.setConditions(conditions);

        TaxRuleResult rate = new TaxRuleResult();
        rate.setResultKey("rate");
        rate.setNumericValue(new BigDecimal("18.0000"));
        rule.setResults(List.of(rate));

        return rule;
    }
}
