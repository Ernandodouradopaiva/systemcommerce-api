package br.com.systemcommerce.fiscal.contingency.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_contingencies")
public class FiscalContingency extends AuditableEntity {

    public enum Mode {
        SVC,
        OFFLINE_NFCE,
        EPEC,
        FS_DA,
        OTHER
    }

    public enum Status {
        ACTIVE,
        CLOSED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private FiscalEstablishment.FiscalEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 30)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "started_by")
    private UUID startedBy;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ended_by")
    private UUID endedBy;

    @Column(name = "uf", length = 2)
    private String uf;
}
