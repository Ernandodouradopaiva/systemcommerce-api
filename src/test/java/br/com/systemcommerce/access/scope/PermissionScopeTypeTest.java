package br.com.systemcommerce.access.scope;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermissionScopeTypeTest {

    @Test
    void broaderAbsorbsNarrower() {
        assertTrue(PermissionScopeType.ORGANIZATION.absorbs(PermissionScopeType.STORE));
        assertTrue(PermissionScopeType.STORE.absorbs(PermissionScopeType.OWN_RECORDS));
        assertFalse(PermissionScopeType.OWN_RECORDS.absorbs(PermissionScopeType.ORGANIZATION));
    }
}
