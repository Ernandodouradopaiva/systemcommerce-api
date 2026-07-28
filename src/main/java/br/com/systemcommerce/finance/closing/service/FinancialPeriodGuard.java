package br.com.systemcommerce.finance.closing.service;

import br.com.systemcommerce.finance.closing.entity.FinancialPeriod;
import br.com.systemcommerce.finance.closing.repository.FinancialPeriodRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Impede pagamentos/recebimentos com data em período CLOSED. */
@Service
@RequiredArgsConstructor
public class FinancialPeriodGuard {

    private final FinancialPeriodRepository periodRepository;

    @Transactional(readOnly = true)
    public void assertDateOpen(UUID organizationId, UUID storeId, LocalDate date) {
        if (organizationId == null || date == null) {
            return;
        }
        List<FinancialPeriod> closed;
        if (storeId != null) {
            closed = periodRepository.findClosedCovering(organizationId, storeId, date);
        } else {
            closed = periodRepository.findClosedOrgCovering(organizationId, date);
        }
        if (!closed.isEmpty()) {
            FinancialPeriod p = closed.getFirst();
            throw new BusinessRuleException(
                    "Período financeiro fechado (" + p.getCode() + ": " + p.getStartDate() + " a " + p.getEndDate()
                            + "). Reabra o período para alterar datas neste intervalo.");
        }
    }
}
