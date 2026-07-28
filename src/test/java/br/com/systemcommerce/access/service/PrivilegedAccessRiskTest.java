package br.com.systemcommerce.access.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.systemcommerce.user.entity.Permission;
import org.junit.jupiter.api.Test;

class PrivilegedAccessRiskTest {

    @Test
    void criticalRequiresApproval() {
        PrivilegedAccessService service = new PrivilegedAccessService(
                null, null, null, null, null, null, null, null, null);
        Permission p = new Permission();
        p.setRiskLevel("CRITICAL");
        p.setSensitive(true);
        assertTrue(service.requiresApproval(p));

        Permission low = new Permission();
        low.setRiskLevel("LOW");
        low.setSensitive(false);
        low.setRequiresDualApproval(false);
        assertFalse(service.requiresApproval(low));
    }
}
