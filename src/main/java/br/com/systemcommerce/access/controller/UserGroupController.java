package br.com.systemcommerce.access.controller;

import br.com.systemcommerce.access.dto.UserGroupDtos.AssignGroupRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.AssignMultipleGroupsRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.EffectivePermissionsResponse;
import br.com.systemcommerce.access.dto.UserGroupDtos.SetPrimaryGroupRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.SetValidityRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.UserGroupAssignmentResponse;
import br.com.systemcommerce.access.entity.AccessChangeHistory;
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
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "User Groups", description = "Participação do usuário em grupos")
public class UserGroupController {

    private final UserGroupAssignmentService userGroupAssignmentService;

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE') or hasAuthority('USER_READ') or hasAuthority('ACCESS_GROUP_READ')")
    @Operation(summary = "Grupos do usuário")
    public ResponseEntity<ApiResponse<List<UserGroupAssignmentResponse>>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.listByUser(userId)));
    }

    @GetMapping("/effective-permissions")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE') or hasAuthority('USER_READ') or hasAuthority('ACCESS_GROUP_READ')")
    @Operation(summary = "Permissões efetivas do usuário")
    public ResponseEntity<ApiResponse<EffectivePermissionsResponse>> effective(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.effectivePermissions(userId)));
    }

    @GetMapping("/access-history")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE') or hasAuthority('AUDIT_READ')")
    @Operation(summary = "Histórico de acesso do usuário")
    public ResponseEntity<ApiResponse<List<AccessChangeHistory>>> history(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.history(userId)));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona usuário a um grupo")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> assign(
            @PathVariable UUID userId, @Valid @RequestBody AssignGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(userGroupAssignmentService.assign(userId, request)));
    }

    @PostMapping("/groups/batch")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Atribui vários grupos")
    public ResponseEntity<ApiResponse<List<UserGroupAssignmentResponse>>> assignMultiple(
            @PathVariable UUID userId, @Valid @RequestBody AssignMultipleGroupsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.assignMultiple(userId, request)));
    }

    @PutMapping("/groups/primary")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Define grupo principal")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> setPrimary(
            @PathVariable UUID userId, @Valid @RequestBody SetPrimaryGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.setPrimary(userId, request)));
    }

    @DeleteMapping("/group-assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove vínculo usuário/grupo")
    public void remove(
            @PathVariable UUID userId,
            @PathVariable UUID assignmentId,
            @RequestParam(required = false) String reason) {
        userGroupAssignmentService.remove(assignmentId, reason);
    }

    @PutMapping("/group-assignments/{assignmentId}/validity")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Define vigência do vínculo")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> validity(
            @PathVariable UUID userId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody SetValidityRequest request) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.setValidity(assignmentId, request)));
    }

    @PostMapping("/group-assignments/{assignmentId}/activate")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Ativa vínculo")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> activate(
            @PathVariable UUID userId, @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.activate(assignmentId)));
    }

    @PostMapping("/group-assignments/{assignmentId}/deactivate")
    @PreAuthorize("hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Inativa vínculo")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> deactivate(
            @PathVariable UUID userId, @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(ApiResponse.of(userGroupAssignmentService.deactivate(assignmentId)));
    }
}
