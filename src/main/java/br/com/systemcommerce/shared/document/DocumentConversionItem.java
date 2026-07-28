package br.com.systemcommerce.shared.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_conversion_items")
public class DocumentConversionItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversion_id", nullable = false)
    private DocumentConversion conversion;

    @Column(name = "from_item_id")
    private UUID fromItemId;

    @Column(name = "to_item_id")
    private UUID toItemId;

    @Column(name = "quantity_source", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantitySource = BigDecimal.ZERO;

    @Column(name = "quantity_converted", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityConverted = BigDecimal.ZERO;

    @Column(name = "quantity_remaining", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityRemaining = BigDecimal.ZERO;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
