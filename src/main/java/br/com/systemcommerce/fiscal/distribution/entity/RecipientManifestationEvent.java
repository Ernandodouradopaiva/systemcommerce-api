package br.com.systemcommerce.fiscal.distribution.entity;

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
        name = "recipient_manifestation_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_rme_manifest_seq", columnNames = {"manifestation_id", "sequence"}))
public class RecipientManifestationEvent extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manifestation_id", nullable = false)
    private RecipientManifestation manifestation;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private RecipientManifestation.ManifestType eventType;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "event_xml", columnDefinition = "TEXT")
    private String eventXml;

    @Column(name = "return_xml", columnDefinition = "TEXT")
    private String returnXml;

    @Column(name = "protocol", length = 60)
    private String protocol;

    @Column(name = "cstat", length = 10)
    private String cstat;

    @Column(name = "xmotivo", length = 255)
    private String xmotivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecipientManifestation.Status status = RecipientManifestation.Status.DRAFT;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;
}
