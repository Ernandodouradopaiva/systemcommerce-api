package br.com.systemcommerce.finance.incomestatement.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "income_statement_lines")
public class IncomeStatementLine extends AuditableEntity {

    public enum LineType {
        HEADER,
        DETAIL,
        TOTAL,
        FORMULA
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private IncomeStatementLayout layout;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 30)
    private LineType lineType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 500)
    private String formula;

    @Column(name = "formula_doc", length = 1000)
    private String formulaDoc;

    @Column(name = "sign_multiplier", nullable = false)
    private Integer signMultiplier = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_line_id")
    private IncomeStatementLine parentLine;
}
