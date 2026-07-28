package br.com.systemcommerce.finance.cashflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_report_export_audits")
public class FinanceReportExportAudit {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "report_type", nullable = false, length = 60)
    private String reportType;

    @Column(name = "export_format", nullable = false, length = 10)
    private String exportFormat;

    @Column(name = "filters_json")
    private String filtersJson;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
