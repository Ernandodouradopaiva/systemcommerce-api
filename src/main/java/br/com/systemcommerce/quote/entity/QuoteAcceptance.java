package br.com.systemcommerce.quote.entity;

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

/** Registro de aceite do orçamento pelo cliente (portal/link público, e-mail, WhatsApp etc.) — Prompt 64. */
@Getter
@Setter
@Entity
@Table(name = "quote_acceptances")
public class QuoteAcceptance {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "accepted_by_name", length = 200)
    private String acceptedByName;

    @Column(name = "accepted_by_email", length = 255)
    private String acceptedByEmail;

    @Column(name = "acceptance_token", length = 80)
    private String acceptanceToken;

    @Column(name = "channel", length = 40)
    private String channel;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (acceptedAt == null) {
            acceptedAt = Instant.now();
        }
    }
}
