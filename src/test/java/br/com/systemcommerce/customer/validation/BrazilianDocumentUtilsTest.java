package br.com.systemcommerce.customer.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class BrazilianDocumentUtilsTest {

    @Test
    void shouldValidateKnownCpfAndCnpj() {
        assertThat(BrazilianDocumentUtils.isValidCpf("52998224725")).isTrue();
        assertThat(BrazilianDocumentUtils.isValidCpf("39053344705")).isTrue();
        assertThat(BrazilianDocumentUtils.isValidCnpj("11222333000181")).isTrue();
        assertThat(BrazilianDocumentUtils.isValidCnpj("34028316000103")).isTrue();
    }

    @Test
    void shouldRejectInvalidDocuments() {
        assertThat(BrazilianDocumentUtils.isValidCpf("11111111111")).isFalse();
        assertThat(BrazilianDocumentUtils.isValidCpf("123")).isFalse();
        assertThat(BrazilianDocumentUtils.isValidCnpj("00000000000000")).isFalse();
    }

    @Test
    void shouldEnforceTypeCompatibility() {
        assertThat(BrazilianDocumentUtils.normalizeAndValidate(Customer.CustomerType.PF, "529.982.247-25"))
                .isEqualTo("52998224725");

        assertThatThrownBy(() -> BrazilianDocumentUtils.normalizeAndValidate(
                        Customer.CustomerType.PF, "11222333000181"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CPF");

        assertThatThrownBy(() -> BrazilianDocumentUtils.normalizeAndValidate(
                        Customer.CustomerType.PJ, "52998224725"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void shouldNormalizeViaPfOrPjFlag() {
        assertThat(BrazilianDocumentUtils.normalizeAndValidatePfOrPj(true, "529.982.247-25"))
                .isEqualTo("52998224725");
        assertThat(BrazilianDocumentUtils.normalizeAndValidatePfOrPj(false, "11.222.333/0001-81"))
                .isEqualTo("11222333000181");
    }

    @Test
    void shouldValidateEmailFormat() {
        BrazilianDocumentUtils.assertValidEmail(null);
        BrazilianDocumentUtils.assertValidEmail(" ");
        BrazilianDocumentUtils.assertValidEmail("ok@example.com");

        assertThatThrownBy(() -> BrazilianDocumentUtils.assertValidEmail("invalido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("E-mail");
    }
}
