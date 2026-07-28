package br.com.systemcommerce.fiscal.distribution.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "dfe_distribution_documents",
        uniqueConstraints = @UniqueConstraint(name = "uk_dfe_doc_est_nsu", columnNames = {"establishment_id", "nsu"}))
public class DfeDistributionDocument extends AuditableEntity {

    public enum Status {
        SUMMARY,
        XML_STORED,
        LINKED,
        IGNORED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private DfeDistributionQuery query;

    @Column(name = "nsu", nullable = false)
    private Long nsu;

    @Column(name = "schema_type", nullable = false, length = 40)
    private String schemaType;

    @Column(name = "access_key", length = 44)
    private String accessKey;

    @Column(name = "xml_content", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String xmlContent;

    @Column(name = "xml_sha256", length = 64)
    private String xmlSha256;

    @Column(name = "summary_json", columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "suspicious", nullable = false)
    private Boolean suspicious = Boolean.FALSE;

    @Column(name = "suspicious_reason", length = 255)
    private String suspiciousReason;

    @Column(name = "recognized", nullable = false)
    private Boolean recognized = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incoming_document_id")
    private IncomingFiscalDocument incomingDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.SUMMARY;
}
