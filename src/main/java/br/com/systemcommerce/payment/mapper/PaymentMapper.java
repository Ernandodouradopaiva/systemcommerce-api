package br.com.systemcommerce.payment.mapper;

import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.PaymentStatusHistoryResponse;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.entity.PaymentStatusHistory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        BigDecimal applied = payment.getAppliedAmount() != null
                ? payment.getAppliedAmount()
                : payment.getAmount();
        BigDecimal informed = payment.getInformedAmount() != null
                ? payment.getInformedAmount()
                : applied;
        BigDecimal change = payment.getChangeAmount() != null
                ? payment.getChangeAmount()
                : resolveChangeLegacy(payment);
        return new PaymentResponse(
                payment.getId(),
                payment.getSale().getId(),
                payment.getSale().getSaleNumber(),
                payment.getCashSession() != null ? payment.getCashSession().getId() : null,
                payment.getMethod(),
                payment.getAmount(),
                informed,
                applied,
                change,
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getExternalReference(),
                payment.getNotes(),
                payment.getInstallments(),
                payment.getTenderedAmount(),
                payment.getAuthorizationCode(),
                payment.getNsu(),
                payment.getCardBrand(),
                payment.getAcquirer(),
                payment.getIdempotencyKey(),
                payment.getResponsibleUser() != null ? payment.getResponsibleUser().getId() : null,
                payment.getResponsibleUser() != null ? payment.getResponsibleUser().getName() : null,
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    public PaymentStatusHistoryResponse toHistoryResponse(PaymentStatusHistory history) {
        return new PaymentStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getReason(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getChangedBy() != null ? history.getChangedBy().getName() : null);
    }

    private BigDecimal resolveChangeLegacy(Payment payment) {
        if (payment.getTenderedAmount() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return payment.getTenderedAmount()
                .subtract(payment.getAmount())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
