package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_statements")
public class BankStatement extends AuditableEntity {
    public enum SourceType { OFX, CSV, MANUAL }
    public enum Status { OPEN, CLOSED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "statement_date", nullable = false) private LocalDate statementDate;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(name = "opening_balance", precision = 18, scale = 2) private BigDecimal openingBalance;
    @Column(name = "closing_balance", precision = 18, scale = 2) private BigDecimal closingBalance;
    @Column(nullable = false, length = 3) private String currency = "BRL";
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 40) private SourceType sourceType;
    @Column(name = "external_file_hash", length = 128) private String externalFileHash;
    @Column(name = "original_payload", columnDefinition = "TEXT") private String originalPayload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(length = 2000) private String notes;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("entryDate ASC")
    private List<BankStatementEntry> entries = new ArrayList<>();
}
