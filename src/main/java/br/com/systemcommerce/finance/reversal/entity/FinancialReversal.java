package br.com.systemcommerce.finance.reversal.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_reversals")
public class FinancialReversal extends AuditableEntity {

    public enum SourceType {
        PAYABLE_SETTLEMENT,
        RECEIVABLE_SETTLEMENT,
        FINANCIAL_TRANSFER,
        FINANCIAL_ENTRY,
        HOLDER_MOVEMENT
    }

    public enum Status {
        DRAFT,
        CONFIRMED,
        CANCELLED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @Column(name = "authorized_by")
    private UUID authorizedBy;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(nullable = false)
    private Boolean partial = Boolean.FALSE;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "reversal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialReversalItem> items = new ArrayList<>();
}
