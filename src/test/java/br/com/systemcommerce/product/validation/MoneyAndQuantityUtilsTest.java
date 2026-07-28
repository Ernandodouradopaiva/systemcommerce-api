package br.com.systemcommerce.product.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyAndQuantityUtilsTest {

    @Test
    void shouldScaleMoneyAndRejectNegative() {
        assertThat(MoneyAndQuantityUtils.money(new BigDecimal("10.456"))).isEqualByComparingTo("10.46");
        assertThatThrownBy(() -> MoneyAndQuantityUtils.money(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldScaleQuantityAndRejectNegative() {
        assertThat(MoneyAndQuantityUtils.quantity(new BigDecimal("1.2345"))).isEqualByComparingTo("1.235");
        assertThatThrownBy(() -> MoneyAndQuantityUtils.quantity(new BigDecimal("-0.1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequirePositiveQuantityForMovements() {
        assertThat(MoneyAndQuantityUtils.positiveQuantity(new BigDecimal("0.001"))).isEqualByComparingTo("0.001");
        assertThatThrownBy(() -> MoneyAndQuantityUtils.positiveQuantity(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }
}
