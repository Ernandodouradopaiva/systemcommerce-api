package br.com.systemcommerce.sale.entity;

import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sale_seller_history")
public class SaleSellerHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_seller_profile_id")
    private SellerProfile previousSellerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_seller_profile_id")
    private SellerProfile newSellerProfile;

    @Column(name = "previous_seller_code", length = 40)
    private String previousSellerCode;

    @Column(name = "new_seller_code", length = 40)
    private String newSellerCode;

    @Column(name = "previous_seller_name", length = 200)
    private String previousSellerName;

    @Column(name = "new_seller_name", length = 200)
    private String newSellerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
