package br.com.systemcommerce.finance.billing.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_slips")
public class BankSlip extends AuditableEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "billing_document_id", nullable = false, unique = true)
    private BillingDocument billingDocument;
    @Column(name = "digitable_line", length = 80) private String digitableLine;
    @Column(length = 80) private String barcode;
    @Column(name = "nosso_numero", length = 40) private String nossoNumero;
    @Column(name = "bank_code", length = 10) private String bankCode;
    @Column(length = 20) private String wallet;
    @Column(name = "registered_at") private Instant registeredAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "paid_amount", precision = 18, scale = 2) private BigDecimal paidAmount;
    @Column(name = "pdf_url", length = 500) private String pdfUrl;
}
