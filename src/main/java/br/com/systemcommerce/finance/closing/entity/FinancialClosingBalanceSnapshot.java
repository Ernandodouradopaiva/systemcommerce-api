package br.com.systemcommerce.finance.closing.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_closing_balance_snapshots")
public class FinancialClosingBalanceSnapshot {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "closing_id", nullable = false)
    private FinancialClosing closing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAmount;
    @Column(name = "holder_code", length = 40) private String holderCode;
    @Column(name = "holder_name", length = 200) private String holderName;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
    }
}
