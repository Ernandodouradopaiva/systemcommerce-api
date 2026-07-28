package br.com.systemcommerce.finance.incomestatement.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "income_statement_executions")
public class IncomeStatementExecution extends AuditableEntity {

    public enum Basis {
        COMPETENCE,
        CASH
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private IncomeStatementLayout layout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Basis basis;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "compare_from")
    private LocalDate compareFrom;

    @Column(name = "compare_to")
    private LocalDate compareTo;

    @Column(nullable = false, length = 60)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    @Column(name = "executed_by")
    private UUID executedBy;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "execution", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<IncomeStatementExecutionLine> lines = new ArrayList<>();
}
