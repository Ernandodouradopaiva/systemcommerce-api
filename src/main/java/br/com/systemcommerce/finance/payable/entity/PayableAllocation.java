package br.com.systemcommerce.finance.payable.entity;

import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payable_allocations")
public class PayableAllocation {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;
    @Column(nullable = false, precision = 8, scale = 4) private BigDecimal percentage;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}