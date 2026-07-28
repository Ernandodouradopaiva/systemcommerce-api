package br.com.systemcommerce.access.dto;

import br.com.systemcommerce.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AccessGroupDtos {

    private AccessGroupDtos() {}

    public record AccessGroupCreateRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            Role.GroupType groupType,
            Role.DefaultScope defaultScope,
            Boolean defaultGroup,
            Boolean allowsAdministration,
            Integer visualPriority,
            UUID organizationId) {}

    public record AccessGroupUpdateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            Role.GroupType groupType,
            Role.DefaultScope defaultScope,
            Boolean defaultGroup,
            Boolean allowsAdministration,
            Integer visualPriority,
            Long version) {}

    public record AccessGroupResponse(
            UUID id,
            String code,
            String name,
            String description,
            Role.GroupType groupType,
            Role.DefaultScope defaultScope,
            Boolean systemGroup,
            Boolean defaultGroup,
            Boolean allowsAdministration,
            Integer visualPriority,
            UUID organizationId,
            Boolean active,
            Long version,
            long userCount,
            long permissionCount,
            java.time.Instant updatedAt) {}

    public record AccessGroupDuplicateRequest(
            @NotBlank @Size(max = 50) String newCode, @NotBlank @Size(max = 100) String newName) {}
}
