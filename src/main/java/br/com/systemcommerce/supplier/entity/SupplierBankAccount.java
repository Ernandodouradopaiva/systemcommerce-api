package br.com.systemcommerce.supplier.entity;

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

/** Dados bancários do fornecedor — acesso restrito a SUPPLIER_BANK_DATA_READ/MANAGE. */
@Getter
@Setter
@Entity
@Table(name = "supplier_bank_accounts")
public class SupplierBankAccount extends AuditableEntity {

    public enum BankAccountType {
        CHECKING,
        SAVINGS
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "agency", length = 20)
    private String agency;

    @Column(name = "account", length = 30)
    private String account;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private BankAccountType accountType;

    @Column(name = "pix_key", length = 140)
    private String pixKey;

    @Column(name = "holder_name", length = 150)
    private String holderName;
}
