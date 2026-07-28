package br.com.systemcommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.mapper.PaymentMapper;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.repository.PaymentStatusHistoryRepository;
import br.com.systemcommerce.pos.cash.service.CashMovementService;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private SaleService saleService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private CashMovementService cashMovementService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldRejectConfirmWhenPaymentMissing() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(saleService, never()).refreshFinancialStatusFromPayments(any());
    }

    @Test
    void shouldRejectConfirmWhenCancelled() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(id);
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(id)).isInstanceOf(BusinessRuleException.class);
    }
}
