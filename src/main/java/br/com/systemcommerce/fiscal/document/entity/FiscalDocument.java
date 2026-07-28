package br.com.systemcommerce.fiscal.document.entity;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_documents",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fiscal_documents_numbering",
                    columnNames = {"establishment_id", "model", "series", "number", "environment"}),
            @UniqueConstraint(
                    name = "uk_fiscal_documents_idempotency",
                    columnNames = {"organization_id", "idempotency_key"})
        })
public class FiscalDocument extends AuditableEntity {

    public enum DocumentDirection {
        IN,
        OUT
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", nullable = false, length = 10)
    private String series;

    @Column(name = "number", nullable = false)
    private Long number;

    @Column(name = "access_key", length = 44)
    private String accessKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private FiscalEstablishment.FiscalEnvironment environment;

    @Column(name = "issue_date_time")
    private Instant issueDateTime;

    @Column(name = "entry_exit_date_time")
    private Instant entryExitDateTime;

    @Column(name = "nature_of_operation", length = 200)
    private String natureOfOperation;

    @Column(name = "purpose", length = 40)
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private FiscalOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    private DocumentDirection direction = DocumentDirection.OUT;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_party_type", length = 20)
    private PartyType recipientPartyType;

    @Column(name = "recipient_party_id")
    private UUID recipientPartyId;

    @Column(name = "recipient_snapshot_json", columnDefinition = "TEXT")
    private String recipientSnapshotJson;

    @Column(name = "emitter_snapshot_json", columnDefinition = "TEXT")
    private String emitterSnapshotJson;

    @Column(name = "carrier_id")
    private UUID carrierId;

    @Column(name = "carrier_snapshot_json", columnDefinition = "TEXT")
    private String carrierSnapshotJson;

    @Column(name = "total_products", precision = 19, scale = 2)
    private BigDecimal totalProducts = BigDecimal.ZERO;

    @Column(name = "total_discount", precision = 19, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "total_freight", precision = 19, scale = 2)
    private BigDecimal totalFreight = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 19, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_invoice", precision = 19, scale = 2)
    private BigDecimal totalInvoice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_calculation_id")
    private TaxCalculation taxCalculation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private FiscalDocumentStatus status = FiscalDocumentStatus.DRAFT;

    @Column(name = "sefaz_cstat", length = 10)
    private String sefazCstat;

    @Column(name = "sefaz_xmotivo", length = 500)
    private String sefazXmotivo;

    @Column(name = "layout_version", length = 40)
    private String layoutVersion;

    @Column(name = "application_version", length = 40)
    private String applicationVersion;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "origin_document_type", length = 40)
    private String originDocumentType;

    @Column(name = "origin_document_id")
    private UUID originDocumentId;

    @Column(name = "contingency", nullable = false)
    private Boolean contingency = Boolean.FALSE;

    /** Importação de histórico externo (Prompt 150) — não gera estoque/financeiro. */
    @Column(name = "external_import", nullable = false)
    private Boolean externalImport = Boolean.FALSE;

    @Column(name = "source_system", length = 60)
    private String sourceSystem;

    @Column(name = "migration_batch_id")
    private UUID migrationBatchId;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<FiscalDocumentItem> items = new ArrayList<>();

    public boolean isImmutable() {
        return status == FiscalDocumentStatus.AUTHORIZED
                || status == FiscalDocumentStatus.AUTHORIZED_PENDING_INTEGRATION
                || status == FiscalDocumentStatus.CANCELLED
                || Boolean.TRUE.equals(externalImport);
    }
}
