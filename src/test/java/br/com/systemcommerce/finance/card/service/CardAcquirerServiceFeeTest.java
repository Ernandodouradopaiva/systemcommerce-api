package br.com.systemcommerce.finance.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.card.dto.CardDtos.RegisterTransactionRequest;
import br.com.systemcommerce.finance.card.entity.Acquirer;
import br.com.systemcommerce.finance.card.entity.CardFeePlan;
import br.com.systemcommerce.finance.card.entity.CardTransaction;
import br.com.systemcommerce.finance.card.repository.*;
import br.com.systemcommerce.finance.reconciliation.repository.BankStatementEntryRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
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
class CardAcquirerServiceFeeTest {

    @Mock private AcquirerRepository acquirerRepository;
    @Mock private CardBrandRepository brandRepository;
    @Mock private CardFeePlanRepository feePlanRepository;
    @Mock private CardTransactionRepository transactionRepository;
    @Mock private CardReceivableScheduleRepository scheduleRepository;
    @Mock private CardSettlementRepository settlementRepository;
    @Mock private CardChargebackRepository chargebackRepository;
    @Mock private BankStatementEntryRepository statementEntryRepository;
    @Mock private OrganizationService organizationService;
    @Mock private StoreService storeService;
    @Mock private SaleRepository saleRepository;
    @Mock private CashSessionRepository cashSessionRepository;
    @Mock private BankFinanceService bankFinanceService;
    @Mock private DomainAuditService domainAuditService;

    private CardAcquirerService service;

    @BeforeEach
    void setUp() {
        service = new CardAcquirerService(
                acquirerRepository,
                brandRepository,
                feePlanRepository,
                transactionRepository,
                scheduleRepository,
                settlementRepository,
                chargebackRepository,
                statementEntryRepository,
                organizationService,
                storeService,
                saleRepository,
                cashSessionRepository,
                bankFinanceService,
                domainAuditService);
    }

    @Test
    void registerCalculatesFeeOnApiAndNeverStoresFullPan() {
        UUID orgId = UUID.randomUUID();
        UUID acquirerId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        Acquirer acquirer = new Acquirer();
        acquirer.setId(acquirerId);
        CardFeePlan plan = new CardFeePlan();
        plan.setId(planId);
        plan.setFeePercent(new BigDecimal("2.50"));
        plan.setFeeFixed(new BigDecimal("0.50"));
        plan.setSettlementDays(30);

        when(organizationService.requireUsable(orgId)).thenReturn(org);
        when(transactionRepository.findByOrganizationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(acquirerRepository.findById(acquirerId)).thenReturn(Optional.of(acquirer));
        when(feePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            CardTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        RegisterTransactionRequest request = new RegisterTransactionRequest(
                orgId,
                null,
                null,
                null,
                acquirerId,
                null,
                planId,
                CardTransaction.Modality.CREDIT,
                1,
                new BigDecimal("100.00"),
                "NSU1",
                "AUTH1",
                "1234",
                null,
                null,
                "idem-1");

        CardTransaction saved = service.register(request);
        assertThat(saved.getFeeAmount()).isEqualByComparingTo("3.00"); // 2.5% + 0.50
        assertThat(saved.getNetAmount()).isEqualByComparingTo("97.00");
        assertThat(saved.getCardLastFour()).isEqualTo("1234");
        assertThat(saved.getSchedules()).hasSize(1);
        assertThat(saved.getSchedules().get(0).getExpectedDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void registerRejectsCardLastFourLongerThan4() {
        UUID orgId = UUID.randomUUID();
        when(transactionRepository.findByOrganizationIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        RegisterTransactionRequest request = new RegisterTransactionRequest(
                orgId,
                null,
                null,
                null,
                UUID.randomUUID(),
                null,
                null,
                CardTransaction.Modality.DEBIT,
                1,
                new BigDecimal("10.00"),
                null,
                null,
                "12345",
                null,
                null,
                "idem-2");
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(BusinessRuleException.class);
    }
}
