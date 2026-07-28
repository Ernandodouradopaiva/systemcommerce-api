package br.com.systemcommerce.integration.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "channel_orders")
public class ChannelOrder extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marketplace_account_id", nullable = false)
    private MarketplaceAccount marketplaceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @Column(name = "external_order_id", nullable = false, length = 120)
    private String externalOrderId;

    @Column(name = "external_status", length = 80)
    private String externalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChannelOrderStatus status = ChannelOrderStatus.RECEIVED;

    @Column(name = "buyer_external_id", length = 120)
    private String buyerExternalId;

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "BRL";

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "converted_at")
    private Instant convertedAt;

    @OneToMany(mappedBy = "channelOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelOrderItem> items = new ArrayList<>();
}
