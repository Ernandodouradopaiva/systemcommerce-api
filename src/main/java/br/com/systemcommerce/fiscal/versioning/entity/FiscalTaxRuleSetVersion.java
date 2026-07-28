package br.com.systemcommerce.fiscal.versioning.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "fiscal_tax_rule_set_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_ftrsv_code", columnNames = "code"))
public class FiscalTaxRuleSetVersion extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_version_id", nullable = false)
    private FiscalLayoutVersion layoutVersion;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "rules_json", columnDefinition = "TEXT")
    private String rulesJson;

    @Column(name = "locked", nullable = false)
    private Boolean locked = Boolean.FALSE;
}
