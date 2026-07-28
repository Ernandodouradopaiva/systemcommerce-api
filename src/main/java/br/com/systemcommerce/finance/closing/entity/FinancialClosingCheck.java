package br.com.systemcommerce.finance.closing.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_closing_checks")
public class FinancialClosingCheck {
    public enum Severity { INFO, WARNING, BLOCKER }

    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "closing_id", nullable = false)
    private FinancialClosing closing;
    @Column(name = "check_code", nullable = false, length = 60) private String checkCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Severity severity;
    @Column(nullable = false) private Boolean passed;
    @Column(nullable = false, length = 1000) private String message;
    @Column(columnDefinition = "TEXT") private String details;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
