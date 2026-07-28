package br.com.systemcommerce.finance.billing.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.finance.receivable.entity.Receivable;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing_documents")
public class BillingDocument extends AuditableEntity {
    public enum BillingType { BOLETO, PIX }
    public enum Status { DRAFT, REGISTERED, PENDING, PAID, OVERDUE, CANCELLED, EXPIRED, REFUNDED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receivable_id") private Receivable receivable;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receivable_installment_id")
    private ReceivableInstallment receivableInstallment;
    @Enumerated(EnumType.STRING) @Column(name = "billing_type", nullable = false, length = 20) private BillingType billingType;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.DRAFT;
    @Column(name = "external_id", length = 120) private String externalId;
    @Column(name = "provider_code", length = 60) private String providerCode;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(length = 2000) private String notes;
    @OneToOne(mappedBy = "billingDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private BankSlip bankSlip;
    @OneToOne(mappedBy = "billingDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private PixCharge pixCharge;
    @OneToMany(mappedBy = "billingDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<BillingStatusHistory> statusHistory = new ArrayList<>();
}
