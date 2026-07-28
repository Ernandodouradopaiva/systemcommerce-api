package br.com.systemcommerce.finance.advance.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "advance_refunds")
public class AdvanceRefund extends AuditableEntity {
    public enum Status { CONFIRMED, REVERSED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_advance_id") private CustomerAdvance customerAdvance;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_advance_id") private SupplierAdvance supplierAdvance;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2) private BigDecimal refundAmount;
    @Column(name = "refund_date", nullable = false) private LocalDate refundDate;
    @Column(nullable = false, length = 500) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_movement_id") private FinancialHolderMovement holderMovement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.CONFIRMED;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
}
