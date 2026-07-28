package br.com.systemcommerce.finance.migration.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_migration_runs")
public class FinanceMigrationRun extends AuditableEntity {
    public enum Status { RUNNING, COMPLETED, FAILED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(name = "dry_run", nullable = false) private Boolean dryRun = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.RUNNING;
    @Column(name = "sales_scanned", nullable = false) private Integer salesScanned = 0;
    @Column(name = "receivables_created", nullable = false) private Integer receivablesCreated = 0;
    @Column(name = "purchases_scanned", nullable = false) private Integer purchasesScanned = 0;
    @Column(name = "payables_created", nullable = false) private Integer payablesCreated = 0;
    @Column(name = "skipped_duplicates", nullable = false) private Integer skippedDuplicates = 0;
    @Column(name = "errors_count", nullable = false) private Integer errorsCount = 0;
    @Column(name = "report_json", columnDefinition = "TEXT") private String reportJson;
    @Column(name = "started_at", nullable = false) private Instant startedAt = Instant.now();
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(name = "started_by") private UUID startedBy;
}
