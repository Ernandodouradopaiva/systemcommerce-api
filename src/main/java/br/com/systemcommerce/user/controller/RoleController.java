package br.com.systemcommerce.user.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.user.dto.PermissionSummaryResponse;
import br.com.systemcommerce.user.dto.RoleSummaryResponse;
import br.com.systemcommerce.user.service.RoleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Roles", description = "Grupos de usuários (legado /roles) e permissões")
public class RoleController {

    private final RoleQueryService roleQueryService;

    @GetMapping("/roles")
    @PreAuthorize(
            "hasAuthority('ROLE_READ') or hasAuthority('ROLE_MANAGE') or hasAuthority('USER_CREATE') or hasAuthority('USER_UPDATE') or hasAuthority('ACCESS_GROUP_READ')")
    @Operation(summary = "Lista grupos de usuários ativos")
    public ResponseEntity<ApiResponse<List<RoleSummaryResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.of(roleQueryService.listActiveRoles()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE') or hasAuthority('ROLE_READ') or hasAuthority('ACCESS_GROUP_READ') or hasAuthority('ACCESS_CATALOG_READ')")
    @Operation(summary = "Lista permissões ativas")
    public ResponseEntity<ApiResponse<List<PermissionSummaryResponse>>> listPermissions() {
        return ResponseEntity.ok(ApiResponse.of(roleQueryService.listActivePermissions()));
    }
}
