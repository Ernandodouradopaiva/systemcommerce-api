package br.com.systemcommerce.finance.receivable.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receivable_installments")
public class ReceivableInstallment extends AuditableEntity {
    public enum Status { OPEN, PARTIALLY_RECEIVED, RECEIVED, OVERDUE, CANCELLED, RENEGOTIATED, WRITTEN_OFF }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;
    @Column(name = "installment_number", nullable = false) private Integer installmentNumber;
    @Column(name = "issue_date", nullable = false) private LocalDate issueDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount;
    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2) private BigDecimal interestAmount = BigDecimal.ZERO;
    @Column(name = "fine_amount", nullable = false, precision = 18, scale = 2) private BigDecimal fineAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "received_amount", nullable = false, precision = 18, scale = 2) private BigDecimal receivedAmount = BigDecimal.ZERO;
    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(name = "nosso_numero", length = 40) private String nossoNumero;
    @Column(name = "billing_code", length = 80) private String billingCode;
    @Column(name = "pix_txid", length = 80) private String pixTxid;
    @Column(name = "boleto_number", length = 80) private String boletoNumber;
    @Column(length = 500) private String notes;

    public void refreshOverdue(LocalDate today) {
        if ((status == Status.OPEN || status == Status.PARTIALLY_RECEIVED) && dueDate.isBefore(today)
                && balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = Status.OVERDUE;
        }
    }
}