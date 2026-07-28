package br.com.systemcommerce.pos.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.dto.CashReconciliationResponse;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashReconciliationCalculatorTest {

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CashPhysicalBalanceCalculator physicalBalanceCalculator;

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private CashReconciliationCalculator calculator;

    @Test
    void shouldComputeExpectedCashAndDifferenceSignals() {
        UUID sessionId = UUID.randomUUID();
        CashSession session = new CashSession();
        session.setId(sessionId);
        session.setOpeningAmount(new BigDecimal("100.00"));
        session.setStatus(CashSession.CashSessionStatus.OPEN);

        when(cashMovementRepository.sumAmountBySessionAndType(sessionId, CashMovement.MovementType.SUPPLY))
                .thenReturn(new BigDecimal("50.00"));
        when(cashMovementRepository.sumAmountBySessionAndType(sessionId, CashMovement.MovementType.WITHDRAWAL))
                .thenReturn(new BigDecimal("20.00"));
        when(paymentRepository.sumAmountByCashSessionIdAndStatus(sessionId, Payment.PaymentStatus.CONFIRMED))
                .thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.sumAmountByCashSessionIdAndStatus(sessionId, Payment.PaymentStatus.CANCELLED))
                .thenReturn(new BigDecimal("10.00"));
        when(paymentRepository.sumAmountByCashSessionIdAndStatus(sessionId, Payment.PaymentStatus.REFUNDED))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumConfirmedGroupedByMethod(sessionId))
                .thenReturn(java.util.List.<Object[]>of(
                        new Object[] {Payment.PaymentMethod.CASH, new BigDecimal("80.00")},
                        new Object[] {Payment.PaymentMethod.PIX, new BigDecimal("120.00")}));
        when(physicalBalanceCalculator.expectedPhysicalCash(sessionId)).thenReturn(new BigDecimal("210.00"));

        CashReconciliationResponse recon = calculator.reconcile(session);

        assertThat(recon.expectedCash()).isEqualByComparingTo("210.00");
        assertThat(recon.expectedGeneral()).isEqualByComparingTo("330.00");
        assertThat(calculator.difference(new BigDecimal("215.00"), recon.expectedCash()))
                .isEqualByComparingTo("5.00");
        assertThat(calculator.difference(new BigDecimal("200.00"), recon.expectedCash()))
                .isEqualByComparingTo("-10.00");
        assertThat(recon.byPaymentMethod()).hasSize(2);
    }
}
