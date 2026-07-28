package br.com.systemcommerce.fiscal.distribution.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "dfe_sequence_controls",
        uniqueConstraints = @UniqueConstraint(name = "uk_dfe_seq_establishment", columnNames = "establishment_id"))
public class DfeSequenceControl extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @Column(name = "last_nsu", nullable = false)
    private Long lastNsu = 0L;

    @Column(name = "max_nsu")
    private Long maxNsu;

    @Column(name = "last_query_at")
    private Instant lastQueryAt;

    @Column(name = "next_allowed_query_at")
    private Instant nextAllowedQueryAt;

    @Column(name = "query_interval_seconds", nullable = false)
    private Integer queryIntervalSeconds = 3600;
}
