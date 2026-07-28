package br.com.systemcommerce.fiscal.storage.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_stored_artifacts",
        uniqueConstraints = @UniqueConstraint(name = "uk_fsa_storage_path", columnNames = "storage_path"))
public class FiscalStoredArtifact extends AuditableEntity {

    public enum ArtifactType {
        GENERATED_XML,
        SIGNED_XML,
        SENT_XML,
        RETURN_XML,
        AUTHORIZED_XML,
        EVENT_XML,
        PROTOCOL,
        DANFE_PDF,
        OTHER
    }

    public enum StorageBackend {
        LOCAL,
        S3,
        DB
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private FiscalDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 40)
    private ArtifactType artifactType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_backend", nullable = false, length = 20)
    private StorageBackend storageBackend = StorageBackend.LOCAL;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "encrypted", nullable = false)
    private Boolean encrypted = Boolean.FALSE;

    @Column(name = "immutable", nullable = false)
    private Boolean immutable = Boolean.FALSE;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;
}
