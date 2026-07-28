package br.com.systemcommerce.access.dto;

import br.com.systemcommerce.access.entity.UserGroupAssignment;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserGroupDtos {

    private UserGroupDtos() {}

    public record AssignGroupRequest(
            @NotNull UUID groupId,
            UUID organizationId,
            UUID storeId,
            Instant validFrom,
            Instant validTo,
            Boolean primaryGroup,
            String reason) {}

    public record AssignMultipleGroupsRequest(@NotNull List<AssignGroupRequest> assignments) {}

    public record SetPrimaryGroupRequest(@NotNull UUID groupId, UUID storeId) {}

    public record SetValidityRequest(Instant validFrom, Instant validTo, String reason) {}

    public record UserGroupAssignmentResponse(
            UUID id,
            UUID userId,
            UUID groupId,
            String groupCode,
            String groupName,
            UUID organizationId,
            UUID storeId,
            Instant validFrom,
            Instant validTo,
            UserGroupAssignment.Status status,
            Boolean primaryGroup,
            String reason,
            Boolean active) {}

    public record EffectivePermissionsResponse(UUID userId, List<String> groupCodes, List<String> permissions) {}
}
