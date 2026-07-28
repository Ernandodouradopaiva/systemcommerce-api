package br.com.systemcommerce.finance.card.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_settlement_items")
public class CardSettlementItem {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "settlement_id", nullable = false)
    private CardSettlement settlement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_id", nullable = false)
    private CardReceivableSchedule schedule;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}
