package br.com.systemcommerce.finance.closing.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_closings")
public class FinancialClosing extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "period_id", nullable = false)
    private FinancialPeriod period;
    @Column(name = "closed_at", nullable = false) private Instant closedAt = Instant.now();
    @Column(name = "closed_by") private UUID closedBy;
    @Column(length = 2000) private String notes;
    @Column(name = "blockers_count", nullable = false) private Integer blockersCount = 0;
    @Column(name = "warnings_count", nullable = false) private Integer warningsCount = 0;
    @OneToMany(mappedBy = "closing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialClosingCheck> checks = new ArrayList<>();
    @OneToMany(mappedBy = "closing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialClosingBalanceSnapshot> balanceSnapshots = new ArrayList<>();
    @OneToMany(mappedBy = "closing", cascade = CascadeType.ALL)
    @OrderBy("reopenedAt DESC")
    private List<FinancialClosingReopening> reopenings = new ArrayList<>();
}
