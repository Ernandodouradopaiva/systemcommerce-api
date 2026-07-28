package br.com.systemcommerce.finance.payable.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payable_origins")
public class PayableOrigin {
    public enum OriginType {
        PURCHASE_ORDER, PURCHASE_RECEIPT, SUPPLIER_INVOICE, FREIGHT,
        MANUAL_EXPENSE, SUPPLIER_RETURN, ADJUSTMENT, ADVANCE, BANK_IMPORT
    }
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;
    @Enumerated(EnumType.STRING) @Column(name = "origin_type", nullable = false, length = 40) private OriginType originType;
    @Column(name = "origin_document_id", nullable = false) private UUID originDocumentId;
    @Column(name = "origin_document_number", length = 60) private String originDocumentNumber;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); }
}