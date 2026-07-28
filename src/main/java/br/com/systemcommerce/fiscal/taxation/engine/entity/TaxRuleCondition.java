package br.com.systemcommerce.fiscal.taxation.engine.entity;

import br.com.systemcommerce.fiscal.taxation.engine.ConditionOperator;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tax_rule_conditions")
public class TaxRuleCondition extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private TaxRule rule;

    @Column(name = "field_name", nullable = false, length = 60)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 20)
    private ConditionOperator operator;

    @Column(name = "value_text", length = 500)
    private String valueText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
