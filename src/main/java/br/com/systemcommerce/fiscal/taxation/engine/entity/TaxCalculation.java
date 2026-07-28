package br.com.systemcommerce.fiscal.taxation.engine.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_calculations")
public class TaxCalculation extends AuditableEntity {

    public enum CalculationStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id")
    private FiscalEstablishment establishment;

    @Column(name = "simulation", nullable = false)
    private Boolean simulation = Boolean.TRUE;

    @Column(name = "origin_document_type", length = 40)
    private String originDocumentType;

    @Column(name = "origin_document_id")
    private java.util.UUID originDocumentId;

    @Column(name = "operation_code", length = 40)
    private String operationCode;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CalculationStatus status = CalculationStatus.COMPLETED;

    @Column(name = "total_products", precision = 19, scale = 2)
    private BigDecimal totalProducts = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 19, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    private String currency = "BRL";

    @Column(name = "trace_summary", columnDefinition = "TEXT")
    private String traceSummary;

    @OneToMany(mappedBy = "calculation", fetch = FetchType.LAZY)
    private List<TaxCalculationItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "calculation", fetch = FetchType.LAZY)
    private List<TaxCalculationTrace> traces = new ArrayList<>();
}
