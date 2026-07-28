package br.com.systemcommerce.pos.cancellation.entity;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cancellation_refunds")
public class CancellationRefund extends AuditableEntity {

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancellation_id", nullable = false)
    private SaleCancellation cancellation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private Payment.PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }
}
