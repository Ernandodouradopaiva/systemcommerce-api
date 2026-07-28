package br.com.systemcommerce.finance.bank.entity;

import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_cashes")
public class FinancialCash {

    public enum CashKind {
        ADMIN,
        POS
    }

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false, unique = true)
    private FinancialAccountHolder holder;

    @Enumerated(EnumType.STRING)
    @Column(name = "cash_kind", nullable = false, length = 20)
    private CashKind cashKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_terminal_id")
    private PosTerminal posTerminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_cash_session_id")
    private CashSession linkedCashSession;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
