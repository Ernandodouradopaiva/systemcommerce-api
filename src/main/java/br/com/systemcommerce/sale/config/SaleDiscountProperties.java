package br.com.systemcommerce.sale.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sale.discount")
public class SaleDiscountProperties {

    /** Percentual máximo de desconto sobre o subtotal da venda (0–100). */
    private BigDecimal maxPercent = new BigDecimal("100");

    /** Valor máximo absoluto de desconto (opcional; null = sem teto absoluto). */
    private BigDecimal maxAmount;

    public BigDecimal getMaxPercent() {
        return maxPercent;
    }

    public void setMaxPercent(BigDecimal maxPercent) {
        if (maxPercent != null && maxPercent.compareTo(BigDecimal.ZERO) >= 0) {
            this.maxPercent = maxPercent;
        }
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}
