package br.com.systemcommerce.fiscal.event.cce.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_correction_letter_sequences",
        uniqueConstraints = @UniqueConstraint(name = "uk_fcls_document", columnNames = {"document_id"}))
public class CorrectionLetterSequence extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "next_sequence", nullable = false)
    private Integer nextSequence = 1;
}
