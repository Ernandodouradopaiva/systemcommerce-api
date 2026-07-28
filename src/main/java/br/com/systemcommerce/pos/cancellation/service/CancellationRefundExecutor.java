package br.com.systemcommerce.pos.cancellation.service;

import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.service.PaymentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isola o estorno financeiro para que falha não marque a TX do cancelamento como rollback-only.
 */
@Service
@RequiredArgsConstructor
public class CancellationRefundExecutor {

    private final PaymentService paymentService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundPayment(UUID paymentId, String reason) {
        paymentService.refund(paymentId, new PaymentRefundRequest("Cancelamento PDV: " + reason));
    }
}
