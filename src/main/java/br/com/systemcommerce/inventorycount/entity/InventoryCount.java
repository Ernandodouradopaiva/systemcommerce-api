package br.com.systemcommerce.inventorycount.entity;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_counts")
public class InventoryCount extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "count_number", nullable = false, unique = true, length = 40, updatable = false)
    private String countNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_type", nullable = false, length = 30)
    private InventoryCountType countType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InventoryCountStatus status = InventoryCountStatus.PLANNED;

    @Column(name = "freeze_balances", nullable = false)
    private Boolean freezeBalances = Boolean.FALSE;

    @Column(name = "hide_theoretical_qty", nullable = false)
    private Boolean hideTheoreticalQty = Boolean.TRUE;

    @Column(name = "require_second_count", nullable = false)
    private Boolean requireSecondCount = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Column(name = "planned_at")
    private Instant plannedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @OneToMany(mappedBy = "inventoryCount", orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<InventoryCountItem> items = new ArrayList<>();

    public void addItem(InventoryCountItem item) {
        items.add(item);
        item.setInventoryCount(this);
    }
}
