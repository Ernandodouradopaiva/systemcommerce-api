package br.com.systemcommerce.fiscal.taxation.engine.entity;

import br.com.systemcommerce.fiscal.taxation.engine.TaxKind;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_rules")
public class TaxRule extends AuditableEntity {

    public enum RuleStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_kind", nullable = false, length = 40)
    private TaxKind taxKind;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RuleStatus status = RuleStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "version_code", length = 40)
    private String versionCode;

    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY)
    private List<TaxRuleCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY)
    private List<TaxRuleResult> results = new ArrayList<>();

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == RuleStatus.ACTIVE;
    }

    public boolean isValidOn(LocalDate date) {
        if (!isUsable()) {
            return false;
        }
        if (date == null) {
            date = LocalDate.now();
        }
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !date.isAfter(validUntil);
    }
}
