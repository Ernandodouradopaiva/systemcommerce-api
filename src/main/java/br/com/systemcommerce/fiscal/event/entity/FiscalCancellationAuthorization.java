package br.com.systemcommerce.fiscal.event.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_cancellation_authorizations")
public class FiscalCancellationAuthorization extends AuditableEntity {

    public enum AuthorizationDecision {
        PENDING,
        APPROVED,
        REJECTED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private FiscalCancellationRequest request;

    @Column(name = "approver_user_id")
    private UUID approverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20)
    private AuthorizationDecision decision = AuthorizationDecision.PENDING;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
