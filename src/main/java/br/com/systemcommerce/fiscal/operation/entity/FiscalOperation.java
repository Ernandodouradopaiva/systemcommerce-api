package br.com.systemcommerce.fiscal.operation.entity;

import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import br.com.systemcommerce.organization.entity.Organization;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_operations")
public class FiscalOperation extends AuditableEntity {

    public enum OperationStatus {
        ACTIVE,
        INACTIVE
    }

    public enum StockDirection {
        OUT,
        IN,
        NONE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "nature_of_operation", length = 200)
    private String natureOfOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 40)
    private CalculationPurpose purpose;

    @Column(name = "allowed_models", length = 20)
    private String allowedModels;

    @Column(name = "default_cfop", length = 10)
    private String defaultCfop;

    @Column(name = "generates_finance", nullable = false)
    private Boolean generatesFinance = Boolean.FALSE;

    @Column(name = "moves_stock", nullable = false)
    private Boolean movesStock = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_direction", length = 20)
    private StockDirection stockDirection = StockDirection.NONE;

    @Column(name = "requires_referenced_document", nullable = false)
    private Boolean requiresReferencedDocument = Boolean.FALSE;

    @Column(name = "allows_final_consumer", nullable = false)
    private Boolean allowsFinalConsumer = Boolean.TRUE;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OperationStatus status = OperationStatus.ACTIVE;

    @OneToMany(mappedBy = "operation", fetch = FetchType.LAZY)
    private List<FiscalOperationRule> rules = new ArrayList<>();

    @OneToMany(mappedBy = "operation", fetch = FetchType.LAZY)
    private List<FiscalOperationItemRule> itemRules = new ArrayList<>();

    @OneToMany(mappedBy = "operation", fetch = FetchType.LAZY)
    private List<FiscalOperationStoreAssignment> storeAssignments = new ArrayList<>();

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == OperationStatus.ACTIVE;
    }

    public boolean isValidOn(LocalDate date) {
        if (!isUsable()) {
            return false;
        }
        if (date == null) {
            date = LocalDate.now();
        }
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !date.isAfter(validUntil);
    }

    public void markActive() {
        this.status = OperationStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = OperationStatus.INACTIVE;
        setActive(false);
    }
}
