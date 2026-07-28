package br.com.systemcommerce.finance.approval.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_approval_requests")
public class FinancialApprovalRequest extends AuditableEntity {
    public enum OperationType {
        HIGH_PAYMENT,
        REVERSAL,
        DISCOUNT,
        TRANSFER,
        PERIOD_REOPEN,
        MANUAL_ENTRY
    }

    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED, EXECUTED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @Enumerated(EnumType.STRING) @Column(name = "operation_type", nullable = false, length = 40)
    private OperationType operationType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.PENDING;
    @Column(name = "source_entity_type", nullable = false, length = 60) private String sourceEntityType;
    @Column(name = "source_entity_id") private UUID sourceEntityId;
    @Column(precision = 18, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "BRL";
    @Column(name = "payload_json", columnDefinition = "TEXT") private String payloadJson;
    @Column(length = 2000) private String reason;
    @Column(name = "decision_notes", length = 2000) private String decisionNotes;
    @Column(name = "requested_by") private UUID requestedBy;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt = Instant.now();
    @Column(name = "decided_by") private UUID decidedBy;
    @Column(name = "decided_at") private Instant decidedAt;
    @Column(name = "executed_at") private Instant executedAt;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
}
