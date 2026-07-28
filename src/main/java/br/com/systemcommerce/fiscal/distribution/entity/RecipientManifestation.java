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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "recipient_manifestations",
        uniqueConstraints = @UniqueConstraint(name = "uk_rm_idempotency", columnNames = "idempotency_key"))
public class RecipientManifestation extends AuditableEntity {

    public enum ManifestType {
        NONE,
        SCIENCE,
        CONFIRMATION,
        UNKNOWN,
        NOT_PERFORMED
    }

    public enum Status {
        DRAFT,
        QUEUED,
        AUTHORIZED,
        REJECTED,
        ERROR
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_document_id")
    private DfeDistributionDocument distributionDocument;

    @Column(name = "access_key", nullable = false, length = 44)
    private String accessKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_type", nullable = false, length = 40)
    private ManifestType currentType = ManifestType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "conclusive", nullable = false)
    private Boolean conclusive = Boolean.FALSE;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "protocol", length = 60)
    private String protocol;

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;
}
