package br.com.systemcommerce.customer.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends AuditableEntity {

    public enum CustomerType {
        PF,
        PJ
    }

    /** BLOCKED: cliente não pode gerar novo pedido/venda; orçamento segue {@link #allowQuoteWhenBlocked}. */
    public enum CustomerStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

    public enum CustomerClassification {
        REGULAR,
        VIP,
        WHOLESALE,
        RESELLER,
        GOVERNMENT,
        OTHER
    }

    public enum RegistrationOrigin {
        ERP,
        POS,
        IMPORT,
        ONLINE,
        OTHER
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 2)
    private CustomerType type;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "document", nullable = false, unique = true, length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "mobile", length = 30)
    private String mobile;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_store_id")
    private Store originStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 30)
    private CustomerClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_origin", nullable = false, length = 20)
    private RegistrationOrigin registrationOrigin = RegistrationOrigin.ERP;

    @Column(name = "commercial_notes", length = 2000)
    private String commercialNotes;

    @Column(name = "credit_limit", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "delinquency_indicator", nullable = false)
    private Boolean delinquencyIndicator = Boolean.FALSE;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "allow_quote_when_blocked", nullable = false)
    private Boolean allowQuoteWhenBlocked = Boolean.TRUE;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    /** Utilizável para nova venda/pedido: precisa estar ACTIVE (BLOCKED e INACTIVE não geram novo pedido). */
    public boolean isUsableForSale() {
        return Boolean.TRUE.equals(getActive()) && status == CustomerStatus.ACTIVE;
    }

    /** Orçamento pode ser permitido mesmo com cliente BLOCKED, conforme flag; INACTIVE nunca permite. */
    public boolean isUsableForQuote() {
        if (!Boolean.TRUE.equals(getActive())) {
            return false;
        }
        if (status == CustomerStatus.ACTIVE) {
            return true;
        }
        return status == CustomerStatus.BLOCKED && Boolean.TRUE.equals(allowQuoteWhenBlocked);
    }

    public void markActive() {
        this.status = CustomerStatus.ACTIVE;
        this.blockedAt = null;
        this.blockedReason = null;
        setActive(true);
    }

    public void markInactive() {
        this.status = CustomerStatus.INACTIVE;
        setActive(false);
    }

    public void markBlocked(String reason) {
        this.status = CustomerStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.blockedReason = reason;
        setActive(true);
    }
}
