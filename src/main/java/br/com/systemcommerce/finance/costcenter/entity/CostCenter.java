package br.com.systemcommerce.finance.costcenter.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cost_centers")
public class CostCenter extends AuditableEntity {

    public enum CostCenterStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CostCenter parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Column(name = "accepts_posting", nullable = false)
    private Boolean acceptsPosting = Boolean.TRUE;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CostCenterStatus status = CostCenterStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == CostCenterStatus.ACTIVE;
    }

    public void markActive() {
        this.status = CostCenterStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = CostCenterStatus.INACTIVE;
        setActive(false);
    }
}
