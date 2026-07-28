package br.com.systemcommerce.carrier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Registro de cálculo de frete (Prompt 73) — histórico simples, sem soft delete/versão. */
@Getter
@Setter
@Entity
@Table(name = "freight_quotations")
public class FreightQuotation {

    public enum Source {
        TABLE,
        MANUAL,
        EXTERNAL
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_mode_id")
    private FreightMode freightMode;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "quote_id")
    private UUID quoteId;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "weight", precision = 18, scale = 4)
    private BigDecimal weight;

    @Column(name = "volume", precision = 18, scale = 4)
    private BigDecimal volume;

    @Column(name = "order_amount", precision = 18, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "calculated_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(name = "manual_override", nullable = false)
    private Boolean manualOverride = Boolean.FALSE;

    @Column(name = "override_amount", precision = 18, scale = 2)
    private BigDecimal overrideAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 40)
    private Source source = Source.TABLE;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;

    @Column(name = "calculated_by")
    private UUID calculatedBy;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
        if (manualOverride == null) {
            manualOverride = Boolean.FALSE;
        }
    }
}
