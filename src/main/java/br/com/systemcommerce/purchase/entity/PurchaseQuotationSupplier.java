package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_quotation_suppliers")
public class PurchaseQuotationSupplier extends AuditableEntity {

    public enum InviteStatus {
        INVITED,
        RESPONDED,
        DECLINED,
        SELECTED,
        NOT_SELECTED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_quotation_id", nullable = false)
    private PurchaseQuotation purchaseQuotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InviteStatus status = InviteStatus.INVITED;

    @Column(name = "notes", length = 1000)
    private String notes;
}
