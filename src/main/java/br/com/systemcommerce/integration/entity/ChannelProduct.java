package br.com.systemcommerce.integration.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "channel_products")
public class ChannelProduct extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marketplace_account_id", nullable = false)
    private MarketplaceAccount marketplaceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Column(name = "external_sku", length = 120)
    private String externalSku;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus = "LINKED";

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
