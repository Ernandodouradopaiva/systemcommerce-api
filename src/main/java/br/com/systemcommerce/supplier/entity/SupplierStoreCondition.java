package br.com.systemcommerce.supplier.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Observações/condições específicas de uma loja para o fornecedor (override opcional dos padrões da organização). */
@Getter
@Setter
@Entity
@Table(name = "supplier_store_conditions")
public class SupplierStoreCondition extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "payment_term_days")
    private Integer paymentTermDays;

    @Column(name = "payment_condition", length = 200)
    private String paymentCondition;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "average_lead_time_days")
    private Integer averageLeadTimeDays;
}
