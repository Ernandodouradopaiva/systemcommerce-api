package br.com.systemcommerce.access.auth;

import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionItem;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.ScopeItem;
import br.com.systemcommerce.access.scope.PermissionScopeType;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StoreScopeResolver {

    public boolean coversStore(EffectivePermissionItem item, UUID storeId) {
        if (item == null || storeId == null) {
            return false;
        }
        for (ScopeItem scope : item.scopes()) {
            if (scope.type() == PermissionScopeType.GLOBAL_SYSTEM
                    || scope.type() == PermissionScopeType.ORGANIZATION) {
                return true;
            }
            if (scope.type() == PermissionScopeType.STORE_GROUP) {
                // STORE_GROUP materializado como STORE individuais em EffectivePermissionService
                continue;
            }
            if (scope.type() == PermissionScopeType.STORE
                    && storeId.equals(scope.storeId())) {
                return true;
            }
            // OWN / TEAM ainda exigem loja acessível + ownership/hierarchy no ResourceAccessResolver
            if (scope.type() == PermissionScopeType.OWN_RECORDS
                    || scope.type() == PermissionScopeType.TEAM_RECORDS) {
                return true;
            }
        }
        return false;
    }
}
