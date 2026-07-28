package br.com.systemcommerce.fiscal.taxation.engine.entity;

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
@Table(name = "tax_rule_results")
public class TaxRuleResult extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private TaxRule rule;

    @Column(name = "result_key", nullable = false, length = 60)
    private String resultKey;

    @Column(name = "result_value", length = 200)
    private String resultValue;

    @Column(name = "numeric_value", precision = 19, scale = 6)
    private BigDecimal numericValue;
}
