package br.com.systemcommerce.sale.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SaleTotalsCalculatorTest {

    @Test
    void shouldCalculateLineTotals() {
        var line = SaleTotalsCalculator.calculateLine(
                new BigDecimal("2"), new BigDecimal("10.00"), new BigDecimal("1.00"));
        assertThat(line.lineSubtotal()).isEqualByComparingTo("20.00");
        assertThat(line.discountAmount()).isEqualByComparingTo("1.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("19.00");
    }

    @Test
    void shouldRejectNegativeFreight() {
        SaleDiscountProperties props = new SaleDiscountProperties();
        assertThatThrownBy(() -> SaleTotalsCalculator.calculateSale(
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("-1"),
                        props))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void shouldRejectDiscountAboveMaxPercent() {
        SaleDiscountProperties props = new SaleDiscountProperties();
        props.setMaxPercent(new BigDecimal("10"));
        assertThatThrownBy(() -> SaleTotalsCalculator.calculateSale(
                        new BigDecimal("100"),
                        new BigDecimal("20"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        props))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("percentual");
    }

    @Test
    void shouldRejectLineDiscountAboveSubtotal() {
        assertThatThrownBy(() -> SaleTotalsCalculator.calculateLine(
                        BigDecimal.ONE, new BigDecimal("10"), new BigDecimal("11")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
