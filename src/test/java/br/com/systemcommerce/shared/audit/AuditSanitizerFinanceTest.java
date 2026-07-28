package br.com.systemcommerce.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditSanitizerFinanceTest {

    @Test
    void redactsBankSecretsAndCardData() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("password", "secret");
        input.put("token", "abc");
        input.put("cardNumber", "4111111111111111");
        input.put("cvv", "123");
        input.put("bankSecret", "x");
        input.put("privateKey", "pk");
        input.put("amount", "10.00");
        input.put("cardLastFour", "1111");

        @SuppressWarnings("unchecked")
        Map<String, Object> clean = (Map<String, Object>) AuditSanitizer.sanitize(input);

        assertThat(clean.get("password")).isEqualTo("[REDACTED]");
        assertThat(clean.get("token")).isEqualTo("[REDACTED]");
        assertThat(clean.get("cardNumber")).isEqualTo("[REDACTED]");
        assertThat(clean.get("cvv")).isEqualTo("[REDACTED]");
        assertThat(clean.get("bankSecret")).isEqualTo("[REDACTED]");
        assertThat(clean.get("privateKey")).isEqualTo("[REDACTED]");
        assertThat(clean.get("amount")).isEqualTo("10.00");
        assertThat(clean.get("cardLastFour")).isEqualTo("1111");
    }
}
