package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "supplier_quotation_response_items")
public class SupplierQuotationResponseItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private SupplierQuotationResponse response;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_item_id", nullable = false)
    private PurchaseQuotationItem quotationItem;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "quantity_available", precision = 18, scale = 4)
    private BigDecimal quantityAvailable;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "brand_offered", length = 120)
    private String brandOffered;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "selected", nullable = false)
    private Boolean selected = Boolean.FALSE;

    @Column(name = "quantity_selected", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantitySelected = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** Custo total oficial do item cotado: unit*qty + frete + imposto − desconto. */
    public BigDecimal computeTotalCost(BigDecimal quantity) {
        return unitPrice
                .multiply(quantity)
                .add(freightAmount)
                .add(taxAmount)
                .subtract(discountAmount);
    }
}
