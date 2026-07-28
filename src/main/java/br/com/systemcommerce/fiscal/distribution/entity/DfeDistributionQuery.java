package br.com.systemcommerce.fiscal.distribution.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.organization.entity.Organization;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dfe_distribution_queries")
public class DfeDistributionQuery extends AuditableEntity {

    public enum Status {
        QUEUED,
        RUNNING,
        SUCCESS,
        ERROR,
        THROTTLED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @Column(name = "requested_nsu", nullable = false)
    private Long requestedNsu = 0L;

    @Column(name = "ult_nsu")
    private Long ultNsu;

    @Column(name = "max_nsu")
    private Long maxNsu;

    @Column(name = "cstat", length = 10)
    private String cstat;

    @Column(name = "xmotivo", length = 255)
    private String xmotivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.QUEUED;

    @Column(name = "documents_count", nullable = false)
    private Integer documentsCount = 0;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
