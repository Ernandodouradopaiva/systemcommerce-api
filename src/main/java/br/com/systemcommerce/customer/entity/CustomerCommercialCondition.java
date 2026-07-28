package br.com.systemcommerce.customer.entity;

import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Condição comercial em nível de organização (não duplica por loja). */
@Getter
@Setter
@Entity
@Table(name = "customer_commercial_conditions")
public class CustomerCommercialCondition extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "payment_term_days")
    private Integer paymentTermDays;

    @Column(name = "payment_condition", length = 100)
    private String paymentCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_table_id")
    private PriceTable priceTable;

    @Column(name = "notes", length = 1000)
    private String notes;
}
