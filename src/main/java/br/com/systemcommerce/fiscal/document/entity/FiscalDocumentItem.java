package br.com.systemcommerce.fiscal.document.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_document_items")
public class FiscalDocumentItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_snapshot_json", columnDefinition = "TEXT")
    private String productSnapshotJson;

    @Column(name = "ncm", length = 10)
    private String ncm;

    @Column(name = "cest", length = 10)
    private String cest;

    @Column(name = "cfop", length = 10)
    private String cfop;

    @Column(name = "quantity", precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_snapshot_json", columnDefinition = "TEXT")
    private String taxSnapshotJson;

    @Column(name = "commercial_uom", length = 10)
    private String commercialUom;

    @Column(name = "taxable_uom", length = 10)
    private String taxableUom;
}
