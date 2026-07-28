package br.com.systemcommerce.finance.renegotiation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_renegotiation_items")
public class FinancialRenegotiationItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "renegotiation_id", nullable = false)
    private FinancialRenegotiation renegotiation;

    @Column(name = "original_installment_id", nullable = false)
    private UUID originalInstallmentId;

    @Column(name = "original_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalBalance;

    @PrePersist
    void pre() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
