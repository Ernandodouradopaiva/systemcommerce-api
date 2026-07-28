package br.com.systemcommerce.inventory.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Saldo oficial por produto + depósito. A loja é sempre derivada do depósito ({@link #store}).
 * Chave lógica: (product_id, warehouse_id) — a organização é a do produto/loja.
 */
@Getter
@Setter
@Entity
@Table(
        name = "inventory",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_inventory_product_warehouse",
                        columnNames = {"product_id", "warehouse_id"}))
public class Inventory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /** Loja derivada do depósito (denormalizada para consulta/isolamento). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Saldo físico (on-hand) no depósito. */
    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "quantity_blocked", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityBlocked = BigDecimal.ZERO;

    /** Entrada pendente neste depósito (em trânsito). */
    @Column(name = "quantity_in_transit", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityInTransit = BigDecimal.ZERO;

    @Column(name = "minimum_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal minimumQuantity = BigDecimal.ZERO;

    @Column(name = "maximum_quantity", precision = 19, scale = 3)
    private BigDecimal maximumQuantity;

    @Column(name = "reorder_point", nullable = false, precision = 19, scale = 3)
    private BigDecimal reorderPoint = BigDecimal.ZERO;
}
