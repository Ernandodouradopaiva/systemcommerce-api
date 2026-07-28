package br.com.systemcommerce.fiscal.storage.entity;

import br.com.systemcommerce.organization.entity.Organization;
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
        name = "fiscal_retention_policies",
        uniqueConstraints = @UniqueConstraint(name = "uk_frp_org_model", columnNames = {"organization_id", "model"}))
public class FiscalRetentionPolicy extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "model", length = 10)
    private String model;

    @Column(name = "retention_years", nullable = false)
    private Integer retentionYears = 5;
}
