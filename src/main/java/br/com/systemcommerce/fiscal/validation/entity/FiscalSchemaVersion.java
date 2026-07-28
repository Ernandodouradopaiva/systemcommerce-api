package br.com.systemcommerce.fiscal.validation.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_schema_versions")
public class FiscalSchemaVersion extends AuditableEntity {

    public enum SchemaStatus {
        DRAFT,
        ACTIVE,
        DEPRECATED
    }

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "layout_version", nullable = false, length = 40)
    private String layoutVersion;

    @Column(name = "schema_namespace", length = 200)
    private String schemaNamespace;

    @Column(name = "xsd_resource_path", length = 500)
    private String xsdResourcePath;

    @Column(name = "xsd_content", columnDefinition = "TEXT")
    private String xsdContent;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SchemaStatus status = SchemaStatus.DRAFT;
}
