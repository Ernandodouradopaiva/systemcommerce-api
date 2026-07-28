package br.com.systemcommerce.finance.bank.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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

/** Instrumento financeiro operacional (Prompt 94) — distinto do plano de contas. */
@Getter
@Setter
@Entity
@Table(name = "financial_account_holders")
public class FinancialAccountHolder extends AuditableEntity {

    public enum HolderType {
        CHECKING,
        SAVINGS,
        PAYMENT_ACCOUNT,
        DIGITAL_WALLET,
        ADMIN_CASH,
        POS_CASH,
        PASS_THROUGH,
        MARKETPLACE
    }

    public enum HolderStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false, length = 40)
    private HolderType holderType;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    @Column(name = "allows_payments", nullable = false)
    private Boolean allowsPayments = Boolean.TRUE;

    @Column(name = "allows_receipts", nullable = false)
    private Boolean allowsReceipts = Boolean.TRUE;

    @Column(name = "allows_reconciliation", nullable = false)
    private Boolean allowsReconciliation = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HolderStatus status = HolderStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == HolderStatus.ACTIVE;
    }

    public void markActive() {
        status = HolderStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        status = HolderStatus.INACTIVE;
        setActive(false);
    }
}
