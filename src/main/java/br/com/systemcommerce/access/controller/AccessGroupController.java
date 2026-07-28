package br.com.systemcommerce.access.controller;

import br.com.systemcommerce.access.dto.AccessCatalogResponse;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupCreateRequest;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupDuplicateRequest;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupResponse;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupUpdateRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.BatchPermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.CopyPermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GrantPermissionRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GroupCompareResponse;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GroupPermissionResponse;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsResult;
import br.com.systemcommerce.access.dto.UserGroupDtos.UserGroupAssignmentResponse;
import br.com.systemcommerce.access.entity.AccessChangeHistory;
import br.com.systemcommerce.access.service.AccessCatalogService;
import br.com.systemcommerce.access.service.AccessGroupService;
import br.com.systemcommerce.access.service.GroupPermissionService;
import br.com.systemcommerce.access.service.UserGroupAssignmentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Access Groups", description = "Grupos de usuários e permissões")
public class AccessGroupController {

    private final AccessCatalogService catalogService;
    private final AccessGroupService accessGroupService;
    private final GroupPermissionService groupPermissionService;
    private final UserGroupAssignmentService userGroupAssignmentService;

    @GetMapping("/access-catalog")
    @PreAuthorize("hasAuthority('ACCESS_CATALOG_READ') or hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ROLE_READ')")
    @Operation(summary = "Catálogo agrupado Módulo → Recurso → Ações")
    public ResponseEntity<ApiResponse<AccessCatalogResponse>> catalog() {
        return ResponseEntity.ok(ApiResponse.of(catalogService.getGroupedCatalog()));
    }

    @GetMapping("/access-groups")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ROLE_READ')")
    @Operation(summary = "Lista/pesquisa grupos de usuários")
    public ResponseEntity<ApiResponse<List<AccessGroupResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.list(search, activeOnly)));
    }

    @GetMapping("/access-groups/{id}")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ROLE_READ')")
    @Operation(summary = "Consulta grupo")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.getById(id)));
    }

    @PostMapping("/access-groups")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria grupo")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> create(@Valid @RequestBody AccessGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(accessGroupService.create(request)));
    }

    @PutMapping("/access-groups/{id}")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_UPDATE')")
    @Operation(summary = "Atualiza grupo")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody AccessGroupUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.update(id, request)));
    }

    @PostMapping("/access-groups/{id}/activate")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_DISABLE')")
    @Operation(summary = "Ativa grupo")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.activate(id)));
    }

    @PostMapping("/access-groups/{id}/deactivate")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_DISABLE')")
    @Operation(summary = "Inativa grupo")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.deactivate(id)));
    }

    @PostMapping("/access-groups/{id}/duplicate")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_DUPLICATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Duplica grupo e permissões")
    public ResponseEntity<ApiResponse<AccessGroupResponse>> duplicate(
            @PathVariable UUID id, @Valid @RequestBody AccessGroupDuplicateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(accessGroupService.duplicate(id, request)));
    }

    @DeleteMapping("/access-groups/{id}")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_DISABLE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclusão lógica do grupo (quando permitido)")
    public void softDelete(@PathVariable UUID id) {
        accessGroupService.softDelete(id);
    }

    @GetMapping("/access-groups/{id}/history")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ')")
    @Operation(summary = "Histórico de alterações do grupo")
    public ResponseEntity<ApiResponse<List<AccessChangeHistory>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessGroupService.history(id)));
    }

    @GetMapping("/access-groups/{id}/users")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Usuários do grupo")
    public ResponseEntity<ApiResponse<List<UserGroupAssignmentResponse>>> users(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.listByGroup(id)));
    }

    @GetMapping("/access-groups/{id}/permissions")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @Operation(summary = "Permissões diretas do grupo")
    public ResponseEntity<ApiResponse<List<GroupPermissionResponse>>> permissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(groupPermissionService.listDirect(id)));
    }

    @PostMapping("/access-groups/{id}/permissions")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona permissão ao grupo")
    public ResponseEntity<ApiResponse<GroupPermissionResponse>> addPermission(
            @PathVariable UUID id, @Valid @RequestBody GrantPermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(groupPermissionService.add(id, request)));
    }

    @DeleteMapping("/access-groups/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove permissão do grupo")
    public void removePermission(
            @PathVariable UUID id,
            @PathVariable UUID permissionId,
            @RequestParam(required = false) String reason) {
        groupPermissionService.remove(id, permissionId, reason);
    }

    @PutMapping("/access-groups/{id}/permissions")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @Operation(summary = "Substitui conjunto de permissões (optimistic lock)")
    public ResponseEntity<ApiResponse<ReplacePermissionsResult>> replacePermissions(
            @PathVariable UUID id, @Valid @RequestBody ReplacePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(groupPermissionService.replace(id, request)));
    }

    @PostMapping("/access-groups/{id}/permissions/batch")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @Operation(summary = "Aplica seleção de permissões em lote")
    public ResponseEntity<ApiResponse<List<GroupPermissionResponse>>> batchPermissions(
            @PathVariable UUID id, @Valid @RequestBody BatchPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(groupPermissionService.applyBatch(id, request)));
    }

    @PostMapping("/access-groups/{id}/permissions/copy")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @Operation(summary = "Copia permissões de outro grupo")
    public ResponseEntity<ApiResponse<List<GroupPermissionResponse>>> copyPermissions(
            @PathVariable UUID id, @Valid @RequestBody CopyPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(groupPermissionService.copyFrom(id, request)));
    }

    @GetMapping("/access-groups/compare")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_READ')")
    @Operation(summary = "Compara permissões de dois grupos")
    public ResponseEntity<ApiResponse<GroupCompareResponse>> compare(
            @RequestParam UUID groupAId, @RequestParam UUID groupBId) {
        return ResponseEntity.ok(ApiResponse.of(groupPermissionService.compare(groupAId, groupBId)));
    }
}
