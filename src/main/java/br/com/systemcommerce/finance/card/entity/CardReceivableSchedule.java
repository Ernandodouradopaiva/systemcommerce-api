package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_receivable_schedules")
public class CardReceivableSchedule extends AuditableEntity {
    public enum Status { SCHEDULED, SETTLED, CANCELLED, DIVERGENT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "card_transaction_id", nullable = false)
    private CardTransaction cardTransaction;
    @Column(name = "installment_number", nullable = false) private Integer installmentNumber;
    @Column(name = "expected_date", nullable = false) private LocalDate expectedDate;
    @Column(name = "gross_amount", nullable = false, precision = 18, scale = 2) private BigDecimal grossAmount;
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2) private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2) private BigDecimal netAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.SCHEDULED;
    @Column(name = "settled_at") private LocalDate settledAt;
}
