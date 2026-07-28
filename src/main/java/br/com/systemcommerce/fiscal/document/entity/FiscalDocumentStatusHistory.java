package br.com.systemcommerce.fiscal.document.entity;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_document_status_histories")
public class FiscalDocumentStatusHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private FiscalDocumentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private FiscalDocumentStatus toStatus;

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    @Column(name = "by_user")
    private UUID byUser;

    @Column(name = "sefaz_cstat", length = 10)
    private String sefazCstat;

    @Column(name = "sefaz_xmotivo", length = 500)
    private String sefazXmotivo;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (at == null) {
            at = Instant.now();
        }
    }
}
