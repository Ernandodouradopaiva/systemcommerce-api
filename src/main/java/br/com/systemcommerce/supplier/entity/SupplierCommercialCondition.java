package br.com.systemcommerce.supplier.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Condições comerciais padrão (nível organização) do fornecedor. */
@Getter
@Setter
@Entity
@Table(name = "supplier_commercial_conditions")
public class SupplierCommercialCondition extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, unique = true)
    private Supplier supplier;

    @Column(name = "payment_term_days")
    private Integer paymentTermDays;

    @Column(name = "payment_condition", length = 200)
    private String paymentCondition;

    @Column(name = "preferred_carrier_name", length = 150)
    private String preferredCarrierName;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "average_lead_time_days")
    private Integer averageLeadTimeDays;

    @Column(name = "notes", length = 2000)
    private String notes;
}
