package br.com.systemcommerce.fiscal.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FiscalAuditServiceTest {

    private FiscalAuditService service;

    @BeforeEach
    void setUp() {
        service = new FiscalAuditService(null, new ObjectMapper());
    }

    @Test
    void secretsAreRedacted() {
        Map<String, Object> sanitized =
                service.sanitize(Map.of("certificatePassword", "secret123", "user", "admin"));
        assertThat(sanitized.get("certificatePassword")).isEqualTo("***REDACTED***");
        assertThat(sanitized.get("user")).isEqualTo("admin");
    }

    @Test
    void fullXmlNotStored() {
        Map<String, Object> sanitized = service.sanitize(Map.of("xml", "<NFe><infNFe/></NFe>"));
        @SuppressWarnings("unchecked")
        Map<String, Object> xmlMeta = (Map<String, Object>) sanitized.get("xml");
        assertThat(xmlMeta.get("omitted")).isEqualTo(true);
        assertThat(xmlMeta).doesNotContainKey("content");
    }
}
