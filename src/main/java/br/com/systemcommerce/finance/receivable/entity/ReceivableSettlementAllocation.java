package br.com.systemcommerce.finance.receivable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receivable_settlement_allocations")
public class ReceivableSettlementAllocation {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "settlement_id", nullable = false)
    private ReceivableSettlement settlement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "installment_id", nullable = false)
    private ReceivableInstallment installment;
    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2) private BigDecimal principalAmount;
    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2) private BigDecimal interestAmount = BigDecimal.ZERO;
    @Column(name = "fine_amount", nullable = false, precision = 18, scale = 2) private BigDecimal fineAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "allocated_total", nullable = false, precision = 18, scale = 2) private BigDecimal allocatedTotal;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}