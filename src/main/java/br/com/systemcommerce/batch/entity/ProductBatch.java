package br.com.systemcommerce.batch.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_batches")
public class ProductBatch extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "batch_code", nullable = false, length = 80)
    private String batchCode;

    @Column(name = "manufactured_at")
    private LocalDate manufacturedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_receipt_id")
    private PurchaseReceipt purchaseReceipt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductBatchStatus status = ProductBatchStatus.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;
}
