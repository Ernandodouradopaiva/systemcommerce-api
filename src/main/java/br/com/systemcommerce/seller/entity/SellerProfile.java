package br.com.systemcommerce.seller.entity;

import br.com.systemcommerce.employee.entity.Employee;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "seller_profiles")
public class SellerProfile extends AuditableEntity {

    public enum SellerStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "seller_code", nullable = false, length = 40)
    private String sellerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SellerStatus status = SellerStatus.ACTIVE;

    @Column(name = "max_discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal maxDiscountPercent = BigDecimal.ZERO;

    @Column(name = "allows_external_sale", nullable = false)
    private boolean allowsExternalSale;

    @Column(name = "allows_other_stores", nullable = false)
    private boolean allowsOtherStores;

    @Column(name = "monthly_target_amount", precision = 19, scale = 2)
    private BigDecimal monthlyTargetAmount;

    /** Comissão padrão (%) — Prompt 63. */
    @Column(name = "default_commission_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal defaultCommissionPercent = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_employee_id")
    private Employee supervisor;

    @Column(name = "enabled_at", nullable = false)
    private LocalDate enabledAt = LocalDate.now();

    @Column(name = "disabled_at")
    private LocalDate disabledAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    public boolean isEnabledForSales() {
        return Boolean.TRUE.equals(getActive())
                && status == SellerStatus.ACTIVE
                && disabledAt == null
                && employee != null
                && employee.isOperationallyActive()
                && !employee.isTerminated();
    }

    public void disable() {
        this.status = SellerStatus.INACTIVE;
        this.disabledAt = LocalDate.now();
        setActive(false);
    }

    public void enable() {
        this.status = SellerStatus.ACTIVE;
        this.disabledAt = null;
        this.enabledAt = LocalDate.now();
        setActive(true);
    }
}
