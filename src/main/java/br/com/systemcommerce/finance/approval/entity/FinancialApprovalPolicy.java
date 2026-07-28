package br.com.systemcommerce.finance.approval.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_approval_policies")
public class FinancialApprovalPolicy extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(name = "require_payment_approval", nullable = false) private Boolean requirePaymentApproval = false;
    @Column(name = "payment_approval_threshold", nullable = false, precision = 18, scale = 2)
    private BigDecimal paymentApprovalThreshold = BigDecimal.ZERO;
    @Column(name = "require_reversal_approval", nullable = false) private Boolean requireReversalApproval = true;
    @Column(name = "require_discount_approval", nullable = false) private Boolean requireDiscountApproval = true;
    @Column(name = "discount_approval_threshold", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountApprovalThreshold = BigDecimal.ZERO;
    @Column(name = "require_transfer_approval", nullable = false) private Boolean requireTransferApproval = false;
    @Column(name = "transfer_approval_threshold", nullable = false, precision = 18, scale = 2)
    private BigDecimal transferApprovalThreshold = BigDecimal.ZERO;
    @Column(name = "require_period_reopen_approval", nullable = false) private Boolean requirePeriodReopenApproval = true;
    @Column(name = "require_manual_entry_approval", nullable = false) private Boolean requireManualEntryApproval = false;
    @Column(name = "manual_entry_approval_threshold", nullable = false, precision = 18, scale = 2)
    private BigDecimal manualEntryApprovalThreshold = BigDecimal.ZERO;
}
