package br.com.systemcommerce.finance.paymentcatalog.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_conditions")
public class PaymentCondition extends AuditableEntity {
    public enum ConditionType { CASH, NET_DAYS, INSTALLMENTS, ENTRY_PLUS_INSTALLMENTS, CUSTOM }
    public enum ConditionStatus { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "condition_type", nullable = false, length = 40) private ConditionType conditionType;
    @Column(name = "installment_count", nullable = false) private Integer installmentCount = 1;
    @Column(name = "interval_days", nullable = false) private Integer intervalDays = 0;
    @Column(name = "first_due_days", nullable = false) private Integer firstDueDays = 0;
    @Column(name = "min_amount", precision = 18, scale = 2) private BigDecimal minAmount;
    @Column(name = "allows_purchase", nullable = false) private Boolean allowsPurchase = true;
    @Column(name = "allows_sale", nullable = false) private Boolean allowsSale = true;
    @Column(name = "allows_pos", nullable = false) private Boolean allowsPos = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConditionStatus status = ConditionStatus.ACTIVE;
    @OneToMany(mappedBy = "paymentCondition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceNo ASC")
    private List<PaymentConditionInstallment> installments = new ArrayList<>();
    public boolean isUsable() { return Boolean.TRUE.equals(getActive()) && status == ConditionStatus.ACTIVE; }
    public void markActive() { status = ConditionStatus.ACTIVE; setActive(true); }
    public void markInactive() { status = ConditionStatus.INACTIVE; setActive(false); }
}
