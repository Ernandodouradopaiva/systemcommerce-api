package br.com.systemcommerce.finance.advance.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "advance_applications")
public class AdvanceApplication extends AuditableEntity {
    public enum TargetType { RECEIVABLE, RECEIVABLE_INSTALLMENT, PAYABLE, PAYABLE_INSTALLMENT }
    public enum Status { CONFIRMED, REVERSED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_advance_id") private CustomerAdvance customerAdvance;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_advance_id") private SupplierAdvance supplierAdvance;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 40) private TargetType targetType;
    @Column(name = "target_document_id", nullable = false) private UUID targetDocumentId;
    @Column(name = "target_installment_id") private UUID targetInstallmentId;
    @Column(name = "applied_amount", nullable = false, precision = 18, scale = 2) private BigDecimal appliedAmount;
    @Column(name = "application_date", nullable = false) private LocalDate applicationDate;
    @Column(length = 1000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.CONFIRMED;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
}
