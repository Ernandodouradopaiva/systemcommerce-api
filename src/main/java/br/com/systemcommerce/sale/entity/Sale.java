package br.com.systemcommerce.sale.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
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
@Table(name = "sales")
public class Sale extends AuditableEntity {

    public enum SaleStatus {
        DRAFT,
        SUSPENDED,
        CONFIRMED,
        PAID,
        PARTIALLY_PAID,
        CANCELLED
    }

    public enum SaleChannel {
        ADMIN,
        POS
    }

    @Column(name = "sale_number", nullable = false, unique = true, length = 30, updatable = false)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Snapshot do cliente no momento da associação — edições futuras do cadastro não alteram o histórico. */
    @Column(name = "customer_name_snapshot", length = 200)
    private String customerNameSnapshot;

    @Column(name = "customer_document_snapshot", length = 20)
    private String customerDocumentSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /**
     * Vendedor comercial (SellerProfile) no momento da operação.
     * Preservado historicamente; mudanças futuras de lotação não alteram vendas anteriores.
     * Preparado para rateio multi-vendedor futuro (tabela sale_seller_splits) sem implementar agora.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    private br.com.systemcommerce.seller.entity.SellerProfile sellerProfile;

    @Column(name = "seller_code_snapshot", length = 40)
    private String sellerCodeSnapshot;

    @Column(name = "seller_name_snapshot", length = 200)
    private String sellerNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_user_id")
    private User supervisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_table_id")
    private PriceTable priceTable;

    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaleStatus status = SaleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SaleChannel channel = SaleChannel.ADMIN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id")
    private PosTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id")
    private CashSession cashSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "last_operation_idempotency_key", length = 100)
    private String lastOperationIdempotencyKey;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspend_reason", length = 500)
    private String suspendReason;

    @Column(name = "suspend_expires_at")
    private Instant suspendExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_by_id")
    private User suspendedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_terminal_id")
    private PosTerminal suspendedTerminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edit_lock_owner_id")
    private User editLockOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edit_lock_terminal_id")
    private PosTerminal editLockTerminal;

    @Column(name = "edit_lock_at")
    private Instant editLockAt;

    @Column(name = "edit_lock_token", length = 80)
    private String editLockToken;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "surcharge_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_authorized_by_id")
    private User discountAuthorizedBy;

    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();

    public boolean isDraft() {
        return status == SaleStatus.DRAFT;
    }

    public boolean isSuspended() {
        return status == SaleStatus.SUSPENDED;
    }

    public boolean isCancelled() {
        return status == SaleStatus.CANCELLED;
    }

    public boolean isPos() {
        return channel == SaleChannel.POS;
    }

    public boolean isConfirmedLike() {
        return status == SaleStatus.CONFIRMED
                || status == SaleStatus.PAID
                || status == SaleStatus.PARTIALLY_PAID;
    }

    /**
     * Venda cancelada, suspensa ou já quitada não recebe pagamento.
     * Rascunho do PDV pode receber pagamentos pendentes antes da finalização.
     */
    public boolean canReceivePayment() {
        if (isCancelled() || isSuspended() || status == SaleStatus.PAID) {
            return false;
        }
        if (isDraft()) {
            return isPos();
        }
        return isConfirmedLike();
    }

    public boolean isEditable() {
        return isDraft();
    }
}
