package br.com.systemcommerce.finance.paymentcatalog.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fin_payment_methods")
public class PaymentMethod extends AuditableEntity {
    public enum MethodType { CASH, PIX, DEBIT, CREDIT, BANK_SLIP, TRANSFER, CHECK, VOUCHER, DIGITAL_WALLET, CUSTOMER_CREDIT, OTHER }
    public enum MethodStatus { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "method_type", nullable = false, length = 40) private MethodType methodType;
    @Column(name = "allows_purchase", nullable = false) private Boolean allowsPurchase = true;
    @Column(name = "allows_sale", nullable = false) private Boolean allowsSale = true;
    @Column(name = "allows_pos", nullable = false) private Boolean allowsPos = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MethodStatus status = MethodStatus.ACTIVE;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    public boolean isUsable() { return Boolean.TRUE.equals(getActive()) && status == MethodStatus.ACTIVE; }
    public void markActive() { status = MethodStatus.ACTIVE; setActive(true); }
    public void markInactive() { status = MethodStatus.INACTIVE; setActive(false); }
}
