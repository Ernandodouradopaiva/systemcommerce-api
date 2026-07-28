package br.com.systemcommerce.pos.cancellation.entity;

import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sale_cancellations")
public class SaleCancellation extends AuditableEntity {

    public enum Status {
        REQUESTED,
        AUTHORIZED,
        COMPLETED,
        REJECTED,
        PARTIALLY_FAILED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.REQUESTED;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by_id")
    private User authorizedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_id")
    private User executedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "decision_notes", length = 500)
    private String decisionNotes;

    @Column(name = "failure_detail", length = 1000)
    private String failureDetail;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @OneToMany(mappedBy = "cancellation", fetch = FetchType.LAZY)
    private List<CancellationRefund> refunds = new ArrayList<>();

    public boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.REJECTED;
    }

    public boolean canAuthorize() {
        return status == Status.REQUESTED;
    }

    public boolean canExecute() {
        return status == Status.AUTHORIZED || status == Status.PARTIALLY_FAILED
                || (status == Status.REQUESTED && sale != null && (sale.isDraft() || sale.isSuspended()));
    }

    public boolean canReprocessRefunds() {
        return status == Status.AUTHORIZED || status == Status.PARTIALLY_FAILED;
    }
}
