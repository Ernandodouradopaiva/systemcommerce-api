package br.com.systemcommerce.customerstore.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.seller.entity.SellerProfile;
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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_store_relationships")
public class CustomerStoreRelationship extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "first_service_at")
    private Instant firstServiceAt;

    @Column(name = "last_purchase_at")
    private Instant lastPurchaseAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_seller_profile_id")
    private SellerProfile preferredSellerProfile;

    @Column(name = "local_notes", length = 2000)
    private String localNotes;

    /** Sobrescreve o limite de crédito global do cliente apenas para esta loja; nulo usa o limite global. */
    @Column(name = "credit_limit_override", precision = 18, scale = 2)
    private BigDecimal creditLimitOverride;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStoreRelationshipStatus status = CustomerStoreRelationshipStatus.ACTIVE;

    public boolean isActiveRelationship() {
        return Boolean.TRUE.equals(getActive()) && status == CustomerStoreRelationshipStatus.ACTIVE;
    }

    public void markActive() {
        this.status = CustomerStoreRelationshipStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = CustomerStoreRelationshipStatus.INACTIVE;
        setActive(false);
    }
}
