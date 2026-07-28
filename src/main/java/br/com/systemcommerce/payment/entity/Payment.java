package br.com.systemcommerce.payment.entity;

import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.store.entity.Store;
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

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    public enum PaymentMethod {
        CASH,
        PIX,
        DEBIT_CARD,
        CREDIT_CARD,
        TRANSFER,
        BANK_SLIP,
        VOUCHER,
        OTHER
    }

    public enum PaymentStatus {
        PENDING,
        CONFIRMED,
        CANCELLED,
        REFUNDED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id")
    private CashSession cashSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    /** Valor aplicado à venda (compatibilidade / fonte dos totais). */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "informed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal informedAmount = BigDecimal.ZERO;

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "installments", nullable = false)
    private Integer installments = 1;

    @Column(name = "tendered_amount", precision = 19, scale = 2)
    private BigDecimal tenderedAmount;

    @Column(name = "authorization_code", length = 60)
    private String authorizationCode;

    @Column(name = "nsu", length = 60)
    private String nsu;

    @Column(name = "card_brand", length = 40)
    private String cardBrand;

    @Column(name = "acquirer", length = 60)
    private String acquirer;

    @Column(name = "idempotency_key", length = 100, unique = true, updatable = false)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == PaymentStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == PaymentStatus.CANCELLED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isTerminal() {
        return isCancelled() || isRefunded();
    }

    public boolean canBeConfirmed() {
        return isPending();
    }

    public boolean canBeCancelled() {
        return isPending() || isConfirmed();
    }

    public boolean canBeRemovedAsPending() {
        return isPending();
    }

    public boolean canBeRefunded() {
        return isConfirmed();
    }

    public boolean isCash() {
        return method == PaymentMethod.CASH;
    }
}
