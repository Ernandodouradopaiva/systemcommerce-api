package br.com.systemcommerce.fiscal.taxation.entity;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog.CatalogType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_tax_catalog_versions")
public class FiscalTaxCatalogVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_type", length = 40)
    private CatalogType catalogType;

    @Column(name = "version_code", length = 40)
    private String versionCode;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "imported_at")
    private Instant importedAt = Instant.now();

    @Column(name = "imported_by")
    private UUID importedBy;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "entry_count")
    private Integer entryCount;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }
}
