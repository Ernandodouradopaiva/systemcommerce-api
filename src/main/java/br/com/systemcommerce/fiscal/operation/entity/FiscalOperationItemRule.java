package br.com.systemcommerce.fiscal.operation.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_operation_item_rules")
public class FiscalOperationItemRule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private FiscalOperation operation;

    @Column(name = "ncm_prefix", length = 10)
    private String ncmPrefix;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "cfop_override", length = 10)
    private String cfopOverride;

    @Column(name = "tax_rule_code", length = 40)
    private String taxRuleCode;
}
