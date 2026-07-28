package br.com.systemcommerce.access.auth;

import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionItem;
import br.com.systemcommerce.access.scope.PermissionScopeType;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceAccessResolver {

    private final OwnershipResolver ownershipResolver;
    private final HierarchyResolver hierarchyResolver;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    public boolean canAccess(UUID userId, EffectivePermissionItem item, String resourceType, UUID resourceId) {
        ResourceSnapshot snapshot = ownershipResolver.resolve(resourceType, resourceId);
        if (snapshot == null) {
            throw new ResourceNotFoundException("Recurso", resourceId);
        }
        if (snapshot.storeId() != null && !storeAuthorizationEvaluator.canAccessStore(userId, snapshot.storeId())) {
            return false;
        }

        boolean hasGlobalOrOrg = hasScope(item, PermissionScopeType.GLOBAL_SYSTEM)
                || hasScope(item, PermissionScopeType.ORGANIZATION);
        boolean hasStoreMatch = snapshot.storeId() != null
                && item.scopes().stream()
                        .anyMatch(s -> (s.type() == PermissionScopeType.STORE
                                        || s.type() == PermissionScopeType.STORE_GROUP)
                                && snapshot.storeId().equals(s.storeId()));
        boolean hasOwn = hasScope(item, PermissionScopeType.OWN_RECORDS);
        boolean hasTeam = hasScope(item, PermissionScopeType.TEAM_RECORDS);

        if (hasGlobalOrOrg || hasStoreMatch) {
            return true;
        }
        if (hasTeam && hierarchyResolver.isInTeamScope(userId, snapshot.ownerUserId())) {
            return true;
        }
        return hasOwn && ownershipResolver.isOwner(userId, snapshot);
    }

    private static boolean hasScope(EffectivePermissionItem item, PermissionScopeType type) {
        return item.scopes().stream().anyMatch(s -> s.type() == type);
    }

    public record ResourceSnapshot(UUID resourceId, UUID storeId, UUID ownerUserId, UUID organizationId) {}
}
