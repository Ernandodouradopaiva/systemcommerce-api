package br.com.systemcommerce.shared.document;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Histórico imutável de conversões documento→documento (Prompt 56 / V183).
 * Nunca é apagado ou reescrito após criado.
 */
@Getter
@Setter
@Entity
@Table(name = "document_conversions")
public class DocumentConversion extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_type", nullable = false, length = 40)
    private OriginDocumentType fromType;

    @Column(name = "from_id", nullable = false)
    private UUID fromId;

    @Column(name = "from_number", length = 40)
    private String fromNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_type", nullable = false, length = 40)
    private OriginDocumentType toType;

    @Column(name = "to_id", nullable = false)
    private UUID toId;

    @Column(name = "to_number", length = 40)
    private String toNumber;

    @Column(name = "converted_at", nullable = false)
    private Instant convertedAt;

    @Column(name = "converted_by_user_id")
    private UUID convertedByUserId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "conversion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fromItemId ASC")
    private List<DocumentConversionItem> items = new ArrayList<>();

    public void addItem(DocumentConversionItem item) {
        items.add(item);
        item.setConversion(this);
    }
}
