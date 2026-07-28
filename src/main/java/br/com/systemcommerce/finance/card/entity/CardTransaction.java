package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_transactions")
public class CardTransaction extends AuditableEntity {
    public enum Modality { DEBIT, CREDIT }
    public enum Status { AUTHORIZED, CAPTURED, SCHEDULED, SETTLED, CANCELLED, CHARGEBACK, DIVERGENT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_id") private Sale sale;
    @Column(name = "payment_id") private UUID paymentId;
    @Column(name = "terminal_id") private UUID terminalId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cash_session_id") private CashSession cashSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "acquirer_id", nullable = false)
    private Acquirer acquirer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "card_brand_id") private CardBrand cardBrand;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fee_plan_id") private CardFeePlan feePlan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Modality modality;
    @Column(nullable = false) private Integer installments = 1;
    @Column(name = "gross_amount", nullable = false, precision = 18, scale = 2) private BigDecimal grossAmount;
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2) private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2) private BigDecimal netAmount;
    @Column(length = 60) private String nsu;
    @Column(name = "authorization_code", length = 60) private String authorizationCode;
    /** Apenas últimos 4 dígitos — nunca PAN/CVV completo. */
    @Column(name = "card_last_four", length = 4) private String cardLastFour;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.AUTHORIZED;
    @Column(name = "authorized_at") private Instant authorizedAt;
    @Column(name = "captured_at") private Instant capturedAt;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(length = 2000) private String notes;
    @OneToMany(mappedBy = "cardTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<CardReceivableSchedule> schedules = new ArrayList<>();
}
