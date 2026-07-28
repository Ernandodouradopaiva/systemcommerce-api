package br.com.systemcommerce.inventory.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Movimentação de estoque (tabela {@code stock_movements}). Histórico imutável — sem update/delete.
 */
@Getter
@Setter
@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
public class InventoryMovement {

    public enum MovementType {
        ENTRY,
        EXIT,
        ADJUSTMENT_POSITIVE,
        ADJUSTMENT_NEGATIVE,
        SALE,
        SALE_CANCEL,
        FUTURE_RETURN,
        CORRECTION,
        TRANSFER_OUT,
        TRANSFER_IN,
        TRANSFER_IN_TRANSIT,
        PURCHASE,
        PURCHASE_CANCEL,
        CUSTOMER_RETURN,
        SUPPLIER_RETURN,
        INVENTORY,
        PRODUCTION,
        INTERNAL_CONSUMPTION
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30, updatable = false)
    private MovementType type;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal quantity;

    @Column(name = "previous_quantity", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal previousQuantity;

    @Column(name = "new_quantity", nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal newQuantity;

    /** Origem da movimentação (ex.: MANUAL, SALE, SEED). */
    @Column(name = "reference_type", length = 40, updatable = false)
    private String origin;

    /** Identificador da origem (ex.: id da venda). */
    @Column(name = "reference_id", updatable = false)
    private UUID originId;

    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_reason_id", updatable = false)
    private InventoryAdjustmentReason adjustmentReason;

    @Column(name = "observation", length = 1000, updatable = false)
    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    /** Código da unidade informada pelo operador (ex.: CX), quando diferente da unidade-base. */
    @Column(name = "informed_unit_code", length = 20, updatable = false)
    private String informedUnitCode;

    /** Fator de conversão aplicado da unidade informada para a unidade-base do estoque. */
    @Column(name = "conversion_factor", precision = 24, scale = 10, updatable = false)
    private BigDecimal conversionFactor;

    /** Quantidade já convertida para a unidade-base (igual a {@code quantity} quando não há conversão). */
    @Column(name = "base_quantity", precision = 18, scale = 4, updatable = false)
    private BigDecimal baseQuantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (store == null && warehouse != null) {
            store = warehouse.getStore();
        }
    }
}
