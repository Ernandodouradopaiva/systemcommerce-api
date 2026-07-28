package br.com.systemcommerce.finance.paymentcatalog.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_method_store_configurations")
public class PaymentMethodStoreConfiguration extends AuditableEntity {
    public enum ConfigStatus { ACTIVE, INACTIVE }
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(name = "allows_pos", nullable = false) private Boolean allowsPos = true;
    @Column(name = "max_installments") private Integer maxInstallments;
    @Column(length = 500) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConfigStatus status = ConfigStatus.ACTIVE;
}
