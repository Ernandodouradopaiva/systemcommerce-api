package br.com.systemcommerce.quote.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quote_items")
public class QuoteItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "line_subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /** Quantidade já convertida em pedido de venda (conversão parcial — Prompt 64). */
    @Column(name = "quantity_converted", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityConverted = BigDecimal.ZERO;

    /** Origem do preço aplicado ao item (ex.: CATALOG, PRICE_TABLE, PROMOTIONAL) — informativo. */
    @Column(name = "price_origin", length = 60)
    private String priceOrigin;

    public BigDecimal remainingToConvert() {
        BigDecimal converted = quantityConverted != null ? quantityConverted : BigDecimal.ZERO;
        return quantity.subtract(converted).max(BigDecimal.ZERO);
    }
}
