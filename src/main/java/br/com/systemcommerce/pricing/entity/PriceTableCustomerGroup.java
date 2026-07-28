package br.com.systemcommerce.pricing.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Restrição opcional de tabela de preço a determinados grupos de cliente (Prompt 68). */
@Getter
@Setter
@Entity
@Table(name = "price_table_customer_groups")
public class PriceTableCustomerGroup extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_table_id", nullable = false)
    private PriceTable priceTable;

    @Column(name = "customer_group_code", nullable = false, length = 60)
    private String customerGroupCode;

    @Column(name = "customer_group_name", length = 120)
    private String customerGroupName;
}
