package br.com.systemcommerce.finance.paymentcatalog.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_condition_installments")
public class PaymentConditionInstallment extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_condition_id", nullable = false)
    private PaymentCondition paymentCondition;
    @Column(name = "sequence_no", nullable = false) private Integer sequenceNo;
    @Column(name = "days_offset", nullable = false) private Integer daysOffset = 0;
    @Column(nullable = false, precision = 8, scale = 4) private BigDecimal percentage;
}
