package br.com.systemcommerce.supplier.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Metadados de documentos do fornecedor — sem upload binário (apenas nome/tipo/referência externa). */
@Getter
@Setter
@Entity
@Table(name = "supplier_documents")
public class SupplierDocument extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type", length = 60)
    private String type;

    @Column(name = "file_ref", length = 500)
    private String fileRef;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @PrePersist
    void onSupplierDocumentPrePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
