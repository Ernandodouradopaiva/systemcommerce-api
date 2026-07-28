package br.com.systemcommerce.commission.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
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
@Table(name = "commission_policies")
public class CommissionPolicy extends AuditableEntity {

    public enum PolicyChannel {
        ADMIN,
        POS,
        ANY
    }

    public enum PolicyStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    private SellerProfile sellerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private PolicyChannel channel = PolicyChannel.ANY;

    @Column(name = "percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal percent = BigDecimal.ZERO;

    @Column(name = "fixed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(name = "requires_paid", nullable = false)
    private boolean requiresPaid;

    @Column(name = "applies_on_confirmed", nullable = false)
    private boolean appliesOnConfirmed = true;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == PolicyStatus.ACTIVE;
    }

    public boolean isValidAt(Instant at) {
        if (validFrom != null && at.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !at.isAfter(validTo);
    }

    /** Maior valor = maior especificidade (produto > categoria > vendedor+loja > loja > org). */
    public int specificityScore() {
        if (product != null) {
            return 5;
        }
        if (category != null) {
            return 4;
        }
        if (sellerProfile != null && store != null) {
            return 3;
        }
        if (store != null) {
            return 2;
        }
        return 1;
    }
}
