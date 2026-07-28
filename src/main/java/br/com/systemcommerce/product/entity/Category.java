package br.com.systemcommerce.product.entity;

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

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    public enum CategoryStatus {
        ACTIVE,
        INACTIVE
    }

    @Column(name = "name", nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == CategoryStatus.ACTIVE;
    }

    public void markActive() {
        this.status = CategoryStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = CategoryStatus.INACTIVE;
        setActive(false);
    }
}
