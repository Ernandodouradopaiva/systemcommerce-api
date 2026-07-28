package br.com.systemcommerce.pos.cash.entity;

import br.com.systemcommerce.sale.entity.Sale;
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

/**
 * Movimentação financeira interna do caixa. Confirmada na criação e imutável —
 * estorno somente via nova movimentação inversa.
 */
@Getter
@Setter
@Entity
@Table(name = "cash_movements")
public class CashMovement extends AuditableEntity {

    public enum MovementType {
        OPENING,
        SUPPLY,
        WITHDRAWAL,
        CASH_SALE,
        CASH_REFUND,
        ADJUSTMENT,
        CLOSING
    }

    public enum CashEffect {
        INCREASE,
        DECREASE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false, updatable = false)
    private CashSession cashSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30, updatable = false)
    private MovementType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "description", length = 1000, updatable = false)
    private String description;

    /** Texto livre de motivo (complementar ao cadastro). */
    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", updatable = false)
    private CashMovementReason movementReason;

    @Column(name = "notes", length = 1000, updatable = false)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_id", updatable = false)
    private User executedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by_id", updatable = false)
    private User authorizedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", updatable = false)
    private Sale sale;

    @Column(name = "origin_type", length = 40, updatable = false)
    private String originType;

    @Column(name = "origin_id", updatable = false)
    private java.util.UUID originId;

    @Column(name = "idempotency_key", length = 100, unique = true, updatable = false)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_movement_id", updatable = false)
    private CashMovement reversesMovement;

    /** Vínculo com o movimento do instrumento financeiro (holder) — Prompt 104. */
    @Column(name = "financial_holder_movement_id", updatable = false)
    private java.util.UUID financialHolderMovementId;

    /** Obrigatório quando type = ADJUSTMENT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cash_effect", length = 20, updatable = false)
    private CashEffect cashEffect;

    public boolean affectsPhysicalCash() {
        return switch (type) {
            case OPENING, SUPPLY, WITHDRAWAL, CASH_SALE, CASH_REFUND, ADJUSTMENT -> true;
            case CLOSING -> false;
        };
    }

    public boolean increasesPhysicalCash() {
        return switch (type) {
            case OPENING, SUPPLY, CASH_SALE -> true;
            case WITHDRAWAL, CASH_REFUND -> false;
            case ADJUSTMENT -> cashEffect == CashEffect.INCREASE;
            case CLOSING -> false;
        };
    }
}
