package br.com.systemcommerce.finance.closing.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_closing_reopenings")
public class FinancialClosingReopening extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "closing_id", nullable = false)
    private FinancialClosing closing;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(name = "reopened_at", nullable = false) private Instant reopenedAt = Instant.now();
    @Column(name = "reopened_by") private UUID reopenedBy;
}
