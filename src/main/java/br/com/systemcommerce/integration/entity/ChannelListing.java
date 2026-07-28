package br.com.systemcommerce.integration.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "channel_listings")
public class ChannelListing extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marketplace_account_id", nullable = false)
    private MarketplaceAccount marketplaceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_product_id", nullable = false)
    private ChannelProduct channelProduct;

    @Column(name = "external_listing_id", nullable = false, length = 120)
    private String externalListingId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "published_price", precision = 18, scale = 4)
    private BigDecimal publishedPrice;

    @Column(name = "published_quantity", precision = 18, scale = 4)
    private BigDecimal publishedQuantity;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
