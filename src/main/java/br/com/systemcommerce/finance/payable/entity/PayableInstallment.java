package br.com.systemcommerce.finance.payable.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payable_installments")
public class PayableInstallment extends AuditableEntity {
    public enum Status { OPEN, PARTIALLY_PAID, PAID, OVERDUE, SCHEDULED, CANCELLED, RENEGOTIATED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;
    @Column(name = "installment_number", nullable = false) private Integer installmentNumber;
    @Column(name = "issue_date", nullable = false) private LocalDate issueDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount;
    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2) private BigDecimal interestAmount = BigDecimal.ZERO;
    @Column(name = "fine_amount", nullable = false, precision = 18, scale = 2) private BigDecimal fineAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "settled_amount", nullable = false, precision = 18, scale = 2) private BigDecimal settledAmount = BigDecimal.ZERO;
    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(length = 80) private String barcode;
    @Column(name = "digitable_line", length = 80) private String digitableLine;
    @Column(name = "reference_code", length = 80) private String referenceCode;

    public void refreshOverdue(LocalDate today) {
        if (status == Status.OPEN || status == Status.PARTIALLY_PAID || status == Status.SCHEDULED) {
            if (dueDate.isBefore(today) && balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
                status = Status.OVERDUE;
            }
        }
    }
}