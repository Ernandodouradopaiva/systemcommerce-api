package br.com.systemcommerce.hierarchy.controller;

import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.HierarchyLinkRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.HierarchyLinkResponse;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.PositionResponse;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamCreateRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamManagerRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamMemberRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamResponse;
import br.com.systemcommerce.hierarchy.service.HierarchyService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hierarchy")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Hierarchy", description = "Cargos, equipes e hierarquia organizacional")
public class HierarchyController {

    private final HierarchyService hierarchyService;

    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('HIERARCHY_READ') or hasAuthority('HIERARCHY_MANAGE')")
    @Operation(summary = "Lista cargos")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> positions(
            @RequestParam(required = false) UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : OrganizationService.DEFAULT_ID;
        return ResponseEntity.ok(ApiResponse.of(hierarchyService.listPositions(orgId)));
    }

    @GetMapping("/teams")
    @PreAuthorize("hasAuthority('HIERARCHY_READ') or hasAuthority('HIERARCHY_MANAGE')")
    @Operation(summary = "Lista equipes")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> teams(
            @RequestParam(required = false) UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : OrganizationService.DEFAULT_ID;
        return ResponseEntity.ok(ApiResponse.of(hierarchyService.listTeams(orgId)));
    }

    @PostMapping("/teams")
    @PreAuthorize("hasAuthority('HIERARCHY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria equipe")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(hierarchyService.createTeam(request)));
    }

    @PostMapping("/teams/{teamId}/members")
    @PreAuthorize("hasAuthority('HIERARCHY_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Adiciona membro à equipe")
    public void addMember(@PathVariable UUID teamId, @Valid @RequestBody TeamMemberRequest request) {
        hierarchyService.addMember(teamId, request);
    }

    @PostMapping("/teams/{teamId}/managers")
    @PreAuthorize("hasAuthority('HIERARCHY_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Adiciona gestor à equipe")
    public void addManager(@PathVariable UUID teamId, @Valid @RequestBody TeamManagerRequest request) {
        hierarchyService.addManager(teamId, request);
    }

    @PostMapping("/organizations/{organizationId}/links")
    @PreAuthorize("hasAuthority('HIERARCHY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula usuário na hierarquia")
    public ResponseEntity<ApiResponse<HierarchyLinkResponse>> link(
            @PathVariable UUID organizationId, @Valid @RequestBody HierarchyLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(hierarchyService.linkUser(organizationId, request)));
    }
}
