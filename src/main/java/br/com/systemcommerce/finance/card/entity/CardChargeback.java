package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_chargebacks")
public class CardChargeback extends AuditableEntity {
    public enum Status { OPEN, ADJUSTED, CANCELLED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "card_transaction_id", nullable = false)
    private CardTransaction cardTransaction;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "schedule_id") private CardReceivableSchedule schedule;
    @Column(name = "chargeback_date", nullable = false) private LocalDate chargebackDate;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 500) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(name = "adjustment_entry_id") private UUID adjustmentEntryId;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
}
