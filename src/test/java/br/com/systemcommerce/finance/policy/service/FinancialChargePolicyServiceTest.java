package br.com.systemcommerce.finance.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.policy.dto.FinancialChargePolicyDtos.SimulateRequest;
import br.com.systemcommerce.finance.policy.dto.FinancialChargePolicyDtos.SimulateResponse;
import br.com.systemcommerce.finance.policy.entity.FinancialChargePolicy;
import br.com.systemcommerce.finance.policy.repository.FinancialChargePolicyRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialChargePolicyServiceTest {

    @Mock
    private FinancialChargePolicyRepository policyRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private DomainAuditService domainAuditService;

    private FinancialChargePolicyService service;
    private FinancialChargePolicy policy;

    @BeforeEach
    void setUp() {
        service = new FinancialChargePolicyService(
                policyRepository, organizationService, storeService, domainAuditService);
        policy = new FinancialChargePolicy();
        policy.setId(UUID.randomUUID());
        policy.setCode("PADRAO");
        policy.setInterestType(FinancialChargePolicy.InterestType.SIMPLE_DAILY);
        policy.setInterestRate(new BigDecimal("0.001"));
        policy.setInterestGraceDays(0);
        policy.setPenaltyType(FinancialChargePolicy.PenaltyType.PERCENT);
        policy.setPenaltyPercent(new BigDecimal("2.00"));
        policy.setPenaltyFixedAmount(BigDecimal.ZERO);
        policy.setEarlyDiscountType(FinancialChargePolicy.EarlyDiscountType.PERCENT);
        policy.setEarlyDiscountPercent(new BigDecimal("5.00"));
        policy.setEarlyDiscountDays(5);
        policy.setMaxAuthorizedDiscountPercent(new BigDecimal("10.00"));
        policy.setRequiresDiscountAuthorization(false);
        policy.setRoundingMode(FinancialChargePolicy.RoundingModeType.HALF_UP);
    }

    @Test
    void simulateOverdueInterestAndPenaltyNeverNegative() {
        when(policyRepository.findDetailedById(policy.getId())).thenReturn(Optional.of(policy));
        SimulateResponse r = service.simulate(new SimulateRequest(
                LocalDate.of(2026, 1, 11),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 11),
                policy.getId(),
                null));
        assertThat(r.interest()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.penalty()).isEqualByComparingTo("20.00");
        assertThat(r.total()).isGreaterThan(new BigDecimal("1000.00"));
        assertThat(r.total()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void simulateEarlyPaymentAppliesDiscount() {
        when(policyRepository.findDetailedById(policy.getId())).thenReturn(Optional.of(policy));
        SimulateResponse r = service.simulate(new SimulateRequest(
                LocalDate.of(2026, 1, 10),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 1, 10),
                policy.getId(),
                null));
        assertThat(r.discount()).isEqualByComparingTo("50.00");
        assertThat(r.total()).isEqualByComparingTo("950.00");
        assertThat(r.interest()).isEqualByComparingTo("0.00");
    }
}
