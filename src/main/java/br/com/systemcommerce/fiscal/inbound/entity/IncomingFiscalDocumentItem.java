package br.com.systemcommerce.fiscal.inbound.entity;

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
@Table(name = "incoming_fiscal_document_items")
public class IncomingFiscalDocumentItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incoming_id", nullable = false)
    private IncomingFiscalDocument incoming;

    @Column(name = "line", nullable = false)
    private Integer line;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "external_code", length = 60)
    private String externalCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ncm", length = 10)
    private String ncm;

    @Column(name = "quantity", precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total", precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "matched", nullable = false)
    private Boolean matched = Boolean.FALSE;
}
