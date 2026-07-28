package br.com.systemcommerce.finance.costcenter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cost_center_hierarchy")
public class CostCenterHierarchy {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "ancestor_id", nullable = false)
    private UUID ancestorId;

    @Column(name = "descendant_id", nullable = false)
    private UUID descendantId;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
