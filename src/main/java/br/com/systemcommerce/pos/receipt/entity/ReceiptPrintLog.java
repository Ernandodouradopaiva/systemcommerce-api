package br.com.systemcommerce.pos.receipt.entity;

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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receipt_print_logs")
public class ReceiptPrintLog extends AuditableEntity {

    public enum PrintType {
        OPENING,
        SALE,
        PAYMENT,
        CASH_WITHDRAWAL,
        CASH_SUPPLY,
        CANCELLATION,
        SESSION_CLOSE,
        REPRINT
    }

    public enum PrintLayout {
        THERMAL_58,
        THERMAL_80,
        A4
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "print_type", nullable = false, length = 40)
    private PrintType printType;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "cash_session_id")
    private UUID cashSessionId;

    @Column(name = "cash_movement_id")
    private UUID cashMovementId;

    @Column(name = "sale_cancellation_id")
    private UUID saleCancellationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "copies", nullable = false)
    private Integer copies = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "layout", nullable = false, length = 40)
    private PrintLayout layout;

    @Column(name = "is_reprint", nullable = false)
    private Boolean isReprint = Boolean.FALSE;

    @Column(name = "original_log_id")
    private UUID originalLogId;

    @Column(name = "authentication_id", nullable = false, length = 80)
    private String authenticationId;

    @Column(name = "terminal_id")
    private UUID terminalId;

    @Column(name = "notes", length = 1000)
    private String notes;
}
