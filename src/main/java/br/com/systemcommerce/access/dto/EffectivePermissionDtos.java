package br.com.systemcommerce.access.dto;

import br.com.systemcommerce.access.scope.PermissionScopeType;
import java.util.List;
import java.util.UUID;

public final class EffectivePermissionDtos {

    private EffectivePermissionDtos() {}

    public record EffectivePermissionsResponse(
            UUID userId, UUID organizationId, long accessVersion, List<EffectivePermissionItem> permissions) {}

    public record EffectivePermissionItem(
            String code, List<ScopeItem> scopes, List<GrantedByGroup> grantedByGroups) {}

    public record ScopeItem(PermissionScopeType type, UUID storeId, UUID organizationId) {}

    public record GrantedByGroup(UUID groupId, String groupName, String groupCode) {}

    public record PermissionExplainResponse(
            UUID userId,
            String permissionCode,
            boolean granted,
            List<ScopeItem> scopes,
            List<GrantedByGroup> grantedByGroups,
            String explanation) {}

    public record AccessCheckRequest(String permissionCode, UUID storeId, String resourceType, UUID resourceId) {}

    public record AccessCheckResponse(boolean allowed, String reason) {}

    public record AuthorizedMenuItem(
            String module,
            String item,
            String path,
            String icon,
            int order,
            List<String> requiredPermissions,
            String storeContext,
            List<String> allowedActions) {}

    public record AccessVersionResponse(UUID userId, long accessVersion) {}
}
