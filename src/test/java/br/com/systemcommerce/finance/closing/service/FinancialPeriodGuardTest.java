package br.com.systemcommerce.finance.closing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.closing.entity.FinancialPeriod;
import br.com.systemcommerce.finance.closing.repository.FinancialPeriodRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodGuardTest {

    @Mock
    private FinancialPeriodRepository periodRepository;

    private FinancialPeriodGuard guard;

    @BeforeEach
    void setUp() {
        guard = new FinancialPeriodGuard(periodRepository);
    }

    @Test
    void blocksWhenClosedPeriodCoversDate() {
        UUID orgId = UUID.randomUUID();
        FinancialPeriod period = new FinancialPeriod();
        period.setCode("2026-03");
        period.setStartDate(LocalDate.of(2026, 3, 1));
        period.setEndDate(LocalDate.of(2026, 3, 31));
        period.setStatus(FinancialPeriod.Status.CLOSED);
        when(periodRepository.findClosedOrgCovering(orgId, LocalDate.of(2026, 3, 15)))
                .thenReturn(List.of(period));

        assertThatThrownBy(() -> guard.assertDateOpen(orgId, null, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fechado");
    }
}
