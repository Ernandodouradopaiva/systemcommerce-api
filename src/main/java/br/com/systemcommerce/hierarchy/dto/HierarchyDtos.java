package br.com.systemcommerce.hierarchy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class HierarchyDtos {

    private HierarchyDtos() {}

    public record PositionResponse(UUID id, String code, String name, Integer levelRank) {}

    public record TeamCreateRequest(
            @NotBlank String code, @NotBlank String name, String description, UUID storeId, @NotNull UUID organizationId) {}

    public record TeamResponse(UUID id, String code, String name, UUID organizationId, UUID storeId, Boolean active) {}

    public record TeamMemberRequest(@NotNull UUID userId, UUID positionId, Instant validFrom, Instant validTo) {}

    public record TeamManagerRequest(@NotNull UUID managerUserId, Boolean primaryManager) {}

    public record HierarchyLinkRequest(@NotNull UUID userId, UUID managerUserId, UUID positionId, UUID storeId) {}

    public record HierarchyLinkResponse(
            UUID id, UUID userId, UUID managerUserId, UUID positionId, UUID storeId, String status) {}
}
