package br.com.systemcommerce.fiscal.inbound.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "incoming_fiscal_documents",
        uniqueConstraints = @UniqueConstraint(name = "uk_incoming_access_key", columnNames = {"access_key"}))
public class IncomingFiscalDocument extends AuditableEntity {

    public enum Status {
        IMPORTED,
        VALIDATED,
        LINKED,
        DIVERGENT,
        REJECTED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "access_key", nullable = false, length = 44, unique = true)
    private String accessKey;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", length = 10)
    private String series;

    @Column(name = "number")
    private Long number;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "xml_content", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String xmlContent;

    @Column(name = "xml_sha256", length = 64)
    private String xmlSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.IMPORTED;

    @Column(name = "authorization_protocol", length = 60)
    private String authorizationProtocol;

    @Column(name = "signature_valid")
    private Boolean signatureValid;

    @Column(name = "authorized")
    private Boolean authorized;

    @Column(name = "imported_at")
    private Instant importedAt;
}
