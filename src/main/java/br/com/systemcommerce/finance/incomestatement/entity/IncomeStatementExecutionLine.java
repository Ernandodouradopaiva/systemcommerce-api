package br.com.systemcommerce.finance.incomestatement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "income_statement_execution_lines")
public class IncomeStatementExecutionLine {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private IncomeStatementExecution execution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private IncomeStatementLine line;

    @Column(name = "line_code", nullable = false, length = 40)
    private String lineCode;

    @Column(name = "line_name", nullable = false, length = 200)
    private String lineName;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "compare_amount", precision = 18, scale = 2)
    private BigDecimal compareAmount;

    @Column(name = "variance_amount", precision = 18, scale = 2)
    private BigDecimal varianceAmount;

    @Column(name = "formula_applied", length = 500)
    private String formulaApplied;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
