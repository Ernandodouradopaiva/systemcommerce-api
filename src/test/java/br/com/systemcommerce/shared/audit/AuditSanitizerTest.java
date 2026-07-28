package br.com.systemcommerce.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditSanitizerTest {

    @Test
    void shouldRedactPasswordAndTokenFields() {
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitized = (Map<String, Object>) AuditSanitizer.sanitize(Map.of(
                "login", "admin",
                "password", "Secret@123",
                "accessToken", "jwt-value",
                "nested", Map.of("refreshToken", "r1", "name", "ok")));

        assertThat(sanitized.get("login")).isEqualTo("admin");
        assertThat(sanitized.get("password")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("accessToken")).isEqualTo("[REDACTED]");
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) sanitized.get("nested");
        assertThat(nested.get("refreshToken")).isEqualTo("[REDACTED]");
        assertThat(nested.get("name")).isEqualTo("ok");
    }

    @Test
    void shouldRedactCardRelatedFields() {
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitized = (Map<String, Object>)
                AuditSanitizer.sanitize(Map.of("cardNumber", "4111111111111111", "cvv", "123", "amount", 10));

        assertThat(sanitized.get("cardNumber")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("cvv")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("amount")).isEqualTo(10);
    }

    @Test
    void shouldRedactPanPinAndSecrets() {
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitized = (Map<String, Object>) AuditSanitizer.sanitize(Map.of(
                "pan",
                "4111111111111111",
                "pin",
                "1234",
                "clientSecret",
                "s3cr3t",
                "cardholder",
                "JOHN DOE",
                "method",
                "CREDIT"));

        assertThat(sanitized.get("pan")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("pin")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("clientSecret")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("cardholder")).isEqualTo("[REDACTED]");
        assertThat(sanitized.get("method")).isEqualTo("CREDIT");
    }
}
