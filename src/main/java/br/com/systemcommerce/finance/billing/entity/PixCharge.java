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
@Table(name = "pix_charges")
public class PixCharge extends AuditableEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "billing_document_id", nullable = false, unique = true)
    private BillingDocument billingDocument;
    @Column(length = 50) private String txid;
    @Column(name = "end_to_end_id", length = 50) private String endToEndId;
    @Column(name = "qr_code", columnDefinition = "TEXT") private String qrCode;
    @Column(name = "qr_code_image_url", length = 500) private String qrCodeImageUrl;
    @Column(name = "copy_paste", columnDefinition = "TEXT") private String copyPaste;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "paid_amount", precision = 18, scale = 2) private BigDecimal paidAmount;
}
