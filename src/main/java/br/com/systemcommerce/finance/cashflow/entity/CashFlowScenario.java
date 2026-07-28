package br.com.systemcommerce.finance.cashflow.entity;

import br.com.systemcommerce.organization.entity.Organization;
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
@Table(name = "cash_flow_scenarios")
public class CashFlowScenario extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "inflow_factor", nullable = false, precision = 10, scale = 4)
    private BigDecimal inflowFactor = BigDecimal.ONE;

    @Column(name = "outflow_factor", nullable = false, precision = 10, scale = 4)
    private BigDecimal outflowFactor = BigDecimal.ONE;
}
