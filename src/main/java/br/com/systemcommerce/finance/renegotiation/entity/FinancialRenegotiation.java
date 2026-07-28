package br.com.systemcommerce.finance.renegotiation.entity;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.finance.policy.entity.FinancialChargePolicy;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_renegotiations")
public class FinancialRenegotiation extends AuditableEntity {

    public enum DocumentSide {
        PAYABLE,
        RECEIVABLE
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
    @Column(name = "document_side", nullable = false, length = 20)
    private DocumentSide documentSide;

    @Column(name = "original_document_id", nullable = false)
    private UUID originalDocumentId;

    @Column(name = "new_document_id")
    private UUID newDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @Column(name = "renegotiation_date", nullable = false)
    private LocalDate renegotiationDate;

    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "down_payment_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal downPaymentAmount = BigDecimal.ZERO;

    @Column(name = "advance_application_id")
    private UUID advanceApplicationId;

    @Column(name = "new_total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal newTotalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_condition_id")
    private PaymentCondition paymentCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_policy_id")
    private FinancialChargePolicy chargePolicy;

    @Column(name = "authorization_required", nullable = false)
    private Boolean authorizationRequired = Boolean.FALSE;

    @Column(name = "authorized_by")
    private UUID authorizedBy;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "renegotiation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialRenegotiationItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "renegotiation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialRenegotiationInstallment> newInstallments = new ArrayList<>();
}
