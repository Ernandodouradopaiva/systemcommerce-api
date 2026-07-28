package br.com.systemcommerce.payment.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentFinancialCalculatorTest {

    @Test
    void shouldCalculateBalanceDue() {
        assertThat(PaymentFinancialCalculator.balanceDue(new BigDecimal("100.00"), new BigDecimal("40.00")))
                .isEqualByComparingTo("60.00");
        assertThat(PaymentFinancialCalculator.balanceDue(new BigDecimal("100.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo("0.00");
        assertThat(PaymentFinancialCalculator.balanceDue(new BigDecimal("50.00"), new BigDecimal("80.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateChange() {
        assertThat(PaymentFinancialCalculator.changeAmount(new BigDecimal("100.00"), new BigDecimal("87.50")))
                .isEqualByComparingTo("12.50");
        assertThat(PaymentFinancialCalculator.changeAmount(null, new BigDecimal("10.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldResolveCashApplicationWithChange() {
        var app = PaymentFinancialCalculator.resolveApplication(
                Payment.PaymentMethod.CASH,
                new BigDecimal("87.50"),
                new BigDecimal("100.00"),
                new BigDecimal("87.50"));
        assertThat(app.appliedAmount()).isEqualByComparingTo("87.50");
        assertThat(app.changeAmount()).isEqualByComparingTo("12.50");
        assertThat(app.tenderedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectChangeForNonCash() {
        assertThatThrownBy(() -> PaymentFinancialCalculator.assertChangeOnlyForCash(
                        Payment.PaymentMethod.PIX, new BigDecimal("1.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dinheiro");
    }

    @Test
    void shouldRejectInvalidTenderedOrAmount() {
        assertThatThrownBy(() -> PaymentFinancialCalculator.positiveMoney(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() ->
                        PaymentFinancialCalculator.changeAmount(new BigDecimal("10.00"), new BigDecimal("20.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("menor");
        assertThatThrownBy(() -> PaymentFinancialCalculator.assertDoesNotExceedBalance(
                        new BigDecimal("30.00"), new BigDecimal("20.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("excede");
    }
}
