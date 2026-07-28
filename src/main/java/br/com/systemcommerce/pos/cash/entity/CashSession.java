package br.com.systemcommerce.pos.cash.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cash_sessions")
public class CashSession extends AuditableEntity {

    public enum CashSessionStatus {
        OPEN,
        CLOSING,
        CLOSED,
        CANCELLED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terminal_id", nullable = false)
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "opening_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CashSessionStatus status = CashSessionStatus.OPEN;

    @Column(name = "expected_amount", precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "counted_amount", precision = 19, scale = 2)
    private BigDecimal countedAmount;

    @Column(name = "difference_amount", precision = 19, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "opening_notes", length = 1000)
    private String openingNotes;

    @Column(name = "closing_notes", length = 1000)
    private String closingNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by_id")
    private User authorizedBy;

    @Column(name = "open_idempotency_key", length = 100, unique = true)
    private String openIdempotencyKey;

    @Column(name = "close_idempotency_key", length = 100)
    private String closeIdempotencyKey;

    public boolean isOpen() {
        return status == CashSessionStatus.OPEN;
    }

    public boolean isClosing() {
        return status == CashSessionStatus.CLOSING;
    }

    public boolean isClosed() {
        return status == CashSessionStatus.CLOSED;
    }

    public boolean isCancelled() {
        return status == CashSessionStatus.CANCELLED;
    }

    public boolean acceptsOperations() {
        return isOpen();
    }

    public boolean canStartClosing() {
        return isOpen();
    }

    public boolean canCompleteClose() {
        return isOpen() || isClosing();
    }

    public boolean canCancelOpening() {
        return isOpen();
    }
}
