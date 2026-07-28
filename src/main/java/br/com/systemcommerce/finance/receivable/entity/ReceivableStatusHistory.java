package br.com.systemcommerce.finance.receivable.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receivable_status_history")
public class ReceivableStatusHistory {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;
    @Column(name = "from_status", length = 30) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 30) private String toStatus;
    @Column(length = 500) private String reason;
    @Column(name = "changed_at", nullable = false) private Instant changedAt = Instant.now();
    @Column(name = "changed_by") private UUID changedBy;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}