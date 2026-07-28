package br.com.systemcommerce.pos.warehouse.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Endereço físico final onde o produto é armazenado dentro de um depósito (Prompt 67). */
@Getter
@Setter
@Entity
@Table(name = "storage_locations")
public class StorageLocation extends AuditableEntity {

    public enum LocationStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private WarehouseZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id")
    private WarehouseAisle aisle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private WarehouseRack rack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private WarehouseShelf shelf;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "barcode", length = 80)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LocationStatus status = LocationStatus.ACTIVE;

    @Column(name = "track_balance", nullable = false)
    private Boolean trackBalance = Boolean.FALSE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == LocationStatus.ACTIVE;
    }

    public void markActive() {
        this.status = LocationStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = LocationStatus.INACTIVE;
        setActive(false);
    }
}
