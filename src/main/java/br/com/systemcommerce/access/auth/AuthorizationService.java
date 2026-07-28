package br.com.systemcommerce.access.auth;

import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionItem;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionsResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.ScopeItem;
import br.com.systemcommerce.access.scope.PermissionScopeType;
import br.com.systemcommerce.access.service.EffectivePermissionService;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Autorização central para SpEL / @PreAuthorize (Prompt 158).
 *
 * <pre>
 * &#64;PreAuthorize("@authorizationService.hasPermission('SALES_ORDER_CREATE')")
 * &#64;PreAuthorize("@authorizationService.hasStorePermission('SALES_ORDER_CANCEL', #storeId)")
 * &#64;PreAuthorize("@authorizationService.canAccessResource('SALES_ORDER_READ', 'SALES_ORDER', #orderId)")
 * </pre>
 */
@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final EffectivePermissionService effectivePermissionService;
    private final StoreScopeResolver storeScopeResolver;
    private final ResourceAccessResolver resourceAccessResolver;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    public boolean hasPermission(String permissionCode) {
        UUID userId = CurrentUser.id().orElse(null);
        if (userId == null || permissionCode == null) {
            return deny("missing_user_or_code", permissionCode, null, null);
        }
        EffectivePermissionsResponse eff = effectivePermissionService.forUser(userId, false);
        boolean ok = eff.permissions().stream().anyMatch(p -> p.code().equals(permissionCode));
        return ok || deny("permission", permissionCode, null, null);
    }

    public boolean hasStorePermission(String permissionCode, UUID storeId) {
        UUID userId = CurrentUser.id().orElse(null);
        if (userId == null || permissionCode == null) {
            return deny("missing_user_or_code", permissionCode, storeId, null);
        }
        if (!hasPermission(permissionCode)) {
            return false;
        }
        if (storeId == null) {
            return deny("missing_store", permissionCode, null, null);
        }
        if (!storeAuthorizationEvaluator.canAccessStore(userId, storeId)) {
            return deny("store_access", permissionCode, storeId, null);
        }
        EffectivePermissionItem item = findItem(userId, permissionCode);
        if (item == null) {
            return deny("permission", permissionCode, storeId, null);
        }
        boolean ok = storeScopeResolver.coversStore(item, storeId);
        return ok || deny("store_scope", permissionCode, storeId, null);
    }

    public boolean canAccessResource(String permissionCode, String resourceType, UUID resourceId) {
        UUID userId = CurrentUser.id().orElse(null);
        if (userId == null || permissionCode == null || resourceType == null || resourceId == null) {
            return deny("missing_args", permissionCode, null, resourceId);
        }
        if (!hasPermission(permissionCode)) {
            return false;
        }
        EffectivePermissionItem item = findItem(userId, permissionCode);
        if (item == null) {
            return deny("permission", permissionCode, null, resourceId);
        }
        boolean ok = resourceAccessResolver.canAccess(userId, item, resourceType, resourceId);
        return ok || deny("resource", permissionCode, null, resourceId);
    }

    private EffectivePermissionItem findItem(UUID userId, String code) {
        return effectivePermissionService.forUser(userId, false).permissions().stream()
                .filter(p -> p.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    private boolean deny(String reason, String permissionCode, UUID storeId, UUID resourceId) {
        log.warn(
                "ACCESS_DENIED reason={} permission={} storeId={} resourceId={} userId={}",
                reason,
                permissionCode,
                storeId,
                resourceId,
                CurrentUser.id().orElse(null));
        return false;
    }
}
