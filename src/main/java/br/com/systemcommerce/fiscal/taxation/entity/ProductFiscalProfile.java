package br.com.systemcommerce.fiscal.taxation.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_fiscal_profiles")
public class ProductFiscalProfile extends AuditableEntity {

    public enum ProfileStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "ncm_code", nullable = false, length = 10)
    private String ncmCode;

    @Column(name = "cest_code", length = 10)
    private String cestCode;

    @Column(name = "ex_tipi", length = 10)
    private String exTipi;

    @Column(name = "origin_code", nullable = false, length = 5)
    private String originCode;

    @Column(name = "commercial_uom", length = 10)
    private String commercialUom;

    @Column(name = "taxable_uom", length = 10)
    private String taxableUom;

    @Column(name = "conversion_factor", precision = 19, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Column(name = "gtin_commercial", length = 14)
    private String gtinCommercial;

    @Column(name = "gtin_taxable", length = 14)
    private String gtinTaxable;

    @Column(name = "ipi_framing", length = 20)
    private String ipiFraming;

    @Column(name = "relevant_scale_indicator", length = 20)
    private String relevantScaleIndicator;

    @Column(name = "manufacturer_cnpj", length = 14)
    private String manufacturerCnpj;

    @Column(name = "benefit_code", length = 20)
    private String benefitCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == ProfileStatus.ACTIVE;
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

    public BigDecimal convertQuantity(BigDecimal commercialQty) {
        if (commercialQty == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal factor = conversionFactor != null ? conversionFactor : BigDecimal.ONE;
        return commercialQty.multiply(factor);
    }

    public void markActive() {
        this.status = ProfileStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = ProfileStatus.INACTIVE;
        setActive(false);
    }
}
