package br.com.systemcommerce.quote.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * SalesQuotation / Orçamento de venda (Prompt 64) — não baixa estoque.
 *
 * <p>{@code UNDER_REVIEW} é mantido por compatibilidade com dados/integrações antigas; desde a V197 o valor
 * persistido passou a ser {@code UNDER_ANALYSIS} (mesmo significado — orçamento em análise interna).
 */
@Getter
@Setter
@Entity
@Table(name = "quotes")
public class Quote extends AuditableEntity {

    public enum QuoteStatus {
        DRAFT,
        /** @deprecated substituído por {@link #UNDER_ANALYSIS}; mantido apenas para compatibilidade de leitura. */
        @Deprecated
        UNDER_REVIEW,
        UNDER_ANALYSIS,
        SENT,
        VIEWED,
        NEGOTIATING,
        APPROVED,
        REJECTED,
        CANCELLED,
        EXPIRED,
        PARTIALLY_CONVERTED,
        CONVERTED
    }

    private static final Set<QuoteStatus> EDITABLE = EnumSet.of(
            QuoteStatus.DRAFT,
            QuoteStatus.UNDER_REVIEW,
            QuoteStatus.UNDER_ANALYSIS,
            QuoteStatus.SENT,
            QuoteStatus.VIEWED,
            QuoteStatus.NEGOTIATING);
    private static final Set<QuoteStatus> EXPIRABLE = EnumSet.of(
            QuoteStatus.DRAFT,
            QuoteStatus.SENT,
            QuoteStatus.UNDER_REVIEW,
            QuoteStatus.UNDER_ANALYSIS,
            QuoteStatus.VIEWED,
            QuoteStatus.NEGOTIATING);
    private static final Set<QuoteStatus> CONVERTIBLE = EnumSet.of(
            QuoteStatus.SENT,
            QuoteStatus.VIEWED,
            QuoteStatus.NEGOTIATING,
            QuoteStatus.APPROVED,
            QuoteStatus.PARTIALLY_CONVERTED);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "quote_number", nullable = false, unique = true, length = 40, updatable = false)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "reserve_stock", nullable = false)
    private Boolean reserveStock = Boolean.FALSE;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "converted_sales_order_id")
    private UUID convertedSalesOrderId;

    /** Canal de origem do orçamento (ex.: ERP, POS, MARKETPLACE, WHATSAPP) — texto livre, sem regra na entidade. */
    @Column(name = "channel", length = 30)
    private String channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_table_id")
    private PriceTable priceTable;

    @Column(name = "payment_condition", length = 200)
    private String paymentCondition;

    @Column(name = "carrier_name", length = 200)
    private String carrierName;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "surcharge_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    private SellerProfile sellerProfile;

    /** Dias de validade informados; {@code validUntil} é calculado a partir deste valor na API. */
    @Column(name = "validity_days")
    private Integer validityDays;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<QuoteItem> items = new ArrayList<>();

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean canExpire() {
        return EXPIRABLE.contains(status);
    }

    public boolean canConvert() {
        return CONVERTIBLE.contains(status);
    }

    /** Após o primeiro envio (fora de DRAFT), qualquer edição deve gerar uma revisão auditável. */
    public boolean requiresRevisionOnEdit() {
        return status != QuoteStatus.DRAFT;
    }

    public void addItem(QuoteItem item) {
        items.add(item);
        item.setQuote(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setQuote(null));
        items.clear();
    }
}
