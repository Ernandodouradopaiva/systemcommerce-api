package br.com.systemcommerce.pos.warehouse.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "warehouses",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouses_store_code", columnNames = {"store_id", "code"}))
public class Warehouse extends AuditableEntity {

    public enum WarehouseStatus {
        ACTIVE,
        INACTIVE
    }

    public enum WarehouseType {
        CENTRAL,
        SALE,
        RETURN,
        DAMAGE,
        QUARANTINE,
        VIRTUAL,
        OTHER
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "allows_sale", nullable = false)
    private Boolean allowsSale = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false, length = 30)
    private WarehouseType warehouseType = WarehouseType.SALE;

    @Column(name = "central", nullable = false)
    private Boolean central = Boolean.FALSE;

    @Column(name = "virtual_warehouse", nullable = false)
    private Boolean virtualWarehouse = Boolean.FALSE;

    @Column(name = "blocked_for_movement", nullable = false)
    private Boolean blockedForMovement = Boolean.FALSE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == WarehouseStatus.ACTIVE;
    }

    public boolean isEligibleForPosSale() {
        return isUsable()
                && Boolean.TRUE.equals(allowsSale)
                && !Boolean.TRUE.equals(blockedForMovement)
                && store != null
                && store.isUsable();
    }

    public boolean isMovementAllowed() {
        return isUsable() && !Boolean.TRUE.equals(blockedForMovement);
    }

    public void markActive() {
        this.status = WarehouseStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = WarehouseStatus.INACTIVE;
        setActive(false);
    }
}
