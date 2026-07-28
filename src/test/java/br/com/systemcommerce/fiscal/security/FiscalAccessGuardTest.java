package br.com.systemcommerce.fiscal.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.fiscal.security.service.FiscalAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiscalAccessGuardTest {

    @Test
    void idorAcrossOrganizationsDenied() {
        FiscalAuditService service = new FiscalAuditService(null, new ObjectMapper());
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        assertThatThrownBy(() -> service.assertSameOrganization(orgA, orgB))
                .hasMessageContaining("indevido");
    }
}
