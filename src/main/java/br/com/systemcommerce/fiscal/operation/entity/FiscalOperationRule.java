package br.com.systemcommerce.fiscal.operation.entity;

import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
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
@Table(name = "fiscal_operation_rules")
public class FiscalOperationRule extends AuditableEntity {

    public enum RuleStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private FiscalOperation operation;

    @Column(name = "origin_uf", length = 2)
    private String originUf;

    @Column(name = "dest_uf", length = 2)
    private String destUf;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_indicator", length = 40)
    private TaxpayerIndicator taxpayerIndicator;

    @Column(name = "final_consumer")
    private Boolean finalConsumer;

    @Column(name = "cfop", length = 10)
    private String cfop;

    @Column(name = "tax_rule_code", length = 40)
    private String taxRuleCode;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RuleStatus status = RuleStatus.ACTIVE;
}
