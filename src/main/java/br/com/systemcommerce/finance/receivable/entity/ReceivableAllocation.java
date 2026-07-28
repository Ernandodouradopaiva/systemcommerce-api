package br.com.systemcommerce.finance.receivable.entity;

import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receivable_allocations")
public class ReceivableAllocation {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;
    @Column(nullable = false, precision = 8, scale = 4) private BigDecimal percentage;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}