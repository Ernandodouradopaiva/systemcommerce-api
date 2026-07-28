package br.com.systemcommerce.storeaccess.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
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
@Table(name = "user_store_access")
public class UserStoreAccess extends AuditableEntity {

    public enum AccessType {
        PERMANENT,
        TEMPORARY,
        SUPPORT,
        ADMINISTRATIVE
    }

    public enum AccessStatus {
        ACTIVE,
        REVOKED,
        EXPIRED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "default_store", nullable = false)
    private boolean defaultStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 20)
    private AccessType accessType = AccessType.PERMANENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccessStatus status = AccessStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_id")
    private User grantedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    public boolean isEffectiveOn(LocalDate date) {
        if (status != AccessStatus.ACTIVE || !Boolean.TRUE.equals(getActive())) {
            return false;
        }
        if (date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    public void revoke() {
        this.status = AccessStatus.REVOKED;
        this.defaultStore = false;
        if (endDate == null || endDate.isAfter(LocalDate.now())) {
            this.endDate = LocalDate.now();
        }
    }
}
