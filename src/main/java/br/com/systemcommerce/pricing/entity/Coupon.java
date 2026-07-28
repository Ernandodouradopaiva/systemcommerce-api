package br.com.systemcommerce.pricing.entity;

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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupon extends AuditableEntity {

    public enum Status {
        ACTIVE,
        INACTIVE,
        EXHAUSTED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "max_uses_per_customer")
    private Integer maxUsesPerCustomer;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    public boolean isUsable(Instant at) {
        if (!Boolean.TRUE.equals(getActive()) || status != Status.ACTIVE) {
            return false;
        }
        if (validFrom != null && at.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && at.isAfter(validUntil)) {
            return false;
        }
        return maxUses == null || usedCount < maxUses;
    }

    public void registerUse() {
        this.usedCount = (this.usedCount == null ? 0 : this.usedCount) + 1;
        if (maxUses != null && usedCount >= maxUses) {
            this.status = Status.EXHAUSTED;
        }
    }
}
