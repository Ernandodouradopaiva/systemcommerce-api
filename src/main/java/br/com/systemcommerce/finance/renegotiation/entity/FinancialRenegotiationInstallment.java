package br.com.systemcommerce.finance.renegotiation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_renegotiation_installments")
public class FinancialRenegotiationInstallment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "renegotiation_id", nullable = false)
    private FinancialRenegotiation renegotiation;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "generated_installment_id")
    private UUID generatedInstallmentId;

    @PrePersist
    void pre() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
