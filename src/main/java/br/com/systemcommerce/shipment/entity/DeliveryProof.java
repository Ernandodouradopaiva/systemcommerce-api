package br.com.systemcommerce.shipment.entity;

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

/**
 * Comprovante de entrega — {@code storageRef} é apenas uma referência (chave/URL) para o arquivo
 * armazenado externamente; esta API não guarda o binário (Prompt 72).
 */
@Getter
@Setter
@Entity
@Table(name = "delivery_proofs")
public class DeliveryProof {

    public enum ProofType {
        SIGNATURE,
        PHOTO,
        DOCUMENT,
        OTHER
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 40)
    private ProofType proofType = ProofType.SIGNATURE;

    @Column(name = "storage_ref", nullable = false, length = 500)
    private String storageRef;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (capturedAt == null) {
            capturedAt = Instant.now();
        }
        if (proofType == null) {
            proofType = ProofType.SIGNATURE;
        }
    }
}
