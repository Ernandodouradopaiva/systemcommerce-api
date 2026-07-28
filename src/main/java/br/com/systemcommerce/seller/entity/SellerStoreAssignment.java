package br.com.systemcommerce.seller.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "seller_store_assignments")
public class SellerStoreAssignment extends AuditableEntity {

    public enum AssignmentStatus {
        ACTIVE,
        ENDED,
        REVOKED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "primary_assignment", nullable = false)
    private boolean primaryAssignment;

    @Column(name = "temporary_assignment", nullable = false)
    private boolean temporaryAssignment;

    @Column(name = "allows_register_sale", nullable = false)
    private boolean allowsRegisterSale = true;

    @Column(name = "max_discount_percent", precision = 7, scale = 4)
    private BigDecimal maxDiscountPercent;

    @Column(name = "target_amount", precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "notes", length = 2000)
    private String notes;

    public boolean isEffectiveOn(LocalDate date) {
        if (status != AssignmentStatus.ACTIVE || !Boolean.TRUE.equals(getActive())) {
            return false;
        }
        if (!allowsRegisterSale) {
            return false;
        }
        if (date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    public void end(LocalDate end) {
        this.endDate = end;
        this.status = AssignmentStatus.ENDED;
        this.primaryAssignment = false;
    }

    public void revoke() {
        this.status = AssignmentStatus.REVOKED;
        this.primaryAssignment = false;
        if (endDate == null || endDate.isAfter(LocalDate.now())) {
            this.endDate = LocalDate.now();
        }
    }
}
