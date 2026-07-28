package br.com.systemcommerce.fiscal.taxation.engine.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_calculation_items")
public class TaxCalculationItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculation_id", nullable = false)
    private TaxCalculation calculation;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "ncm", length = 10)
    private String ncm;

    @Column(name = "cest", length = 10)
    private String cest;

    @Column(name = "origin_code", length = 5)
    private String originCode;

    @Column(name = "quantity", precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_breakdown_json", columnDefinition = "TEXT")
    private String taxBreakdownJson;

    @Column(name = "selected_rule_codes", columnDefinition = "TEXT")
    private String selectedRuleCodes;
}
