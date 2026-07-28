package br.com.systemcommerce.fiscal.inbound.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "incoming_fiscal_divergences")
public class IncomingFiscalDivergence extends AuditableEntity {

    public enum DivergenceType {
        QTY,
        VALUE,
        PRODUCT,
        TAX
    }

    public enum DivergenceStatus {
        OPEN,
        ACCEPTED,
        RESOLVED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incoming_id", nullable = false)
    private IncomingFiscalDocument incoming;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private IncomingFiscalDocumentItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "divergence_type", nullable = false, length = 20)
    private DivergenceType divergenceType;

    @Column(name = "expected_json", columnDefinition = "TEXT")
    private String expectedJson;

    @Column(name = "actual_json", columnDefinition = "TEXT")
    private String actualJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DivergenceStatus status = DivergenceStatus.OPEN;
}
