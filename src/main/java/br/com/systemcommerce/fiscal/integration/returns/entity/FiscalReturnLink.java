package br.com.systemcommerce.fiscal.integration.returns.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_return_links")
public class FiscalReturnLink extends AuditableEntity {

    public enum ReturnType {
        SALE,
        PURCHASE
    }

    public enum LinkStatus {
        PENDING,
        AUTHORIZED,
        REJECTED,
        CANCELLED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 20)
    private ReturnType returnType;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_document_id")
    private FiscalDocument fiscalDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_document_id")
    private FiscalDocument originalDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LinkStatus status = LinkStatus.PENDING;
}
