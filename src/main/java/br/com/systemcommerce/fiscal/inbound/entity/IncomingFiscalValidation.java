package br.com.systemcommerce.fiscal.inbound.entity;

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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "incoming_fiscal_validations")
public class IncomingFiscalValidation {

    public enum ValidationResult {
        OK,
        WARN,
        FAIL
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incoming_id", nullable = false)
    private IncomingFiscalDocument incoming;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private ValidationResult result;

    @Column(name = "messages_json", columnDefinition = "TEXT")
    private String messagesJson;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (validatedAt == null) {
            validatedAt = Instant.now();
        }
    }
}
