package br.com.systemcommerce.fiscal.versioning.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_layout_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_flv_code", columnNames = "code"))
public class FiscalLayoutVersion extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "model", nullable = false, length = 10)
    private String model = "ALL";

    @Column(name = "schema_namespace", length = 200)
    private String schemaNamespace;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "feature_flags_json", columnDefinition = "TEXT")
    private String featureFlagsJson;
}
