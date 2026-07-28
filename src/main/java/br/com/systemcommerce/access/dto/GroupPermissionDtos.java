package br.com.systemcommerce.access.dto;

import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GroupPermissionDtos {

    private GroupPermissionDtos() {}

    public record GrantPermissionRequest(
            @NotNull UUID permissionId,
            GroupPermissionAssignment.Scope scope,
            UUID organizationId,
            UUID storeId,
            Instant validFrom,
            Instant validTo,
            String reason) {}

    /**
     * Substituição do conjunto de permissões do grupo.
     * Aceita {@code permissionIds} e/ou {@code permissionCodes}. Ambos vazios/null = remove todas.
     * {@code expectedVersion} também aceita o alias JSON {@code version}.
     */
    public record ReplacePermissionsRequest(
            List<UUID> permissionIds,
            List<String> permissionCodes,
            GroupPermissionAssignment.Scope scope,
            String reason,
            @NotNull @JsonAlias("version") Long expectedVersion) {}

    public record BatchPermissionsRequest(
            @jakarta.validation.constraints.NotEmpty List<UUID> permissionIds,
            String reason,
            @NotNull Long expectedVersion) {}

    public record CopyPermissionsRequest(@NotNull UUID sourceGroupId, String reason, @NotNull Long expectedVersion) {}

    public record GroupPermissionResponse(
            UUID id,
            UUID permissionId,
            String permissionCode,
            String permissionName,
            GroupPermissionAssignment.GrantType grantType,
            GroupPermissionAssignment.Scope scope,
            UUID organizationId,
            UUID storeId,
            Instant validFrom,
            Instant validTo,
            GroupPermissionAssignment.Status status,
            String reason,
            Long version) {}

    public record EffectivePermissionItem(
            UUID id,
            String code,
            String name,
            String module,
            String resource,
            String scope,
            String riskLevel) {}

    public record ReplacePermissionsResult(
            UUID groupId,
            String groupName,
            Long version,
            int totalPermissions,
            List<String> addedPermissions,
            List<String> removedPermissions,
            List<EffectivePermissionItem> effectivePermissions) {}

    public record GroupCompareResponse(
            UUID groupAId, UUID groupBId, List<String> onlyInA, List<String> onlyInB, List<String> shared) {}
}
