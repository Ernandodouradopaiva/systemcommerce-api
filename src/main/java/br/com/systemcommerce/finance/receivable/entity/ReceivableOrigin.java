package br.com.systemcommerce.finance.receivable.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receivable_origins")
public class ReceivableOrigin {
    public enum OriginType {
        SALES_ORDER, SALE, POS, MARKETPLACE, SERVICE, MANUAL_CHARGE, ADVANCE, RENEGOTIATION, ADJUSTMENT
    }
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;
    @Enumerated(EnumType.STRING) @Column(name = "origin_type", nullable = false, length = 40) private OriginType originType;
    @Column(name = "origin_document_id", nullable = false) private UUID originDocumentId;
    @Column(name = "origin_document_number", length = 60) private String originDocumentNumber;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}