package br.com.systemcommerce.fiscal.document.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fiscal_document_references")
public class FiscalDocumentReference extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "ref_type", length = 40)
    private String refType;

    @Column(name = "ref_access_key", length = 44)
    private String refAccessKey;

    @Column(name = "ref_document_id")
    private UUID refDocumentId;
}
