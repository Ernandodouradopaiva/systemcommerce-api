package br.com.systemcommerce.fiscal.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fiscal_document_xmls")
public class FiscalDocumentXml {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "kind", nullable = false, length = 40)
    private String kind;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "stored_at", nullable = false)
    private Instant storedAt = Instant.now();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (storedAt == null) {
            storedAt = Instant.now();
        }
    }
}
