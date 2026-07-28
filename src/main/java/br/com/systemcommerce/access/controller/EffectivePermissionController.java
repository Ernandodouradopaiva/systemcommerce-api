package br.com.systemcommerce.access.controller;

import br.com.systemcommerce.access.dto.EffectivePermissionDtos.AccessCheckRequest;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.AccessCheckResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.AccessVersionResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.AuthorizedMenuItem;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionsResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.PermissionExplainResponse;
import br.com.systemcommerce.access.auth.AuthorizationService;
import br.com.systemcommerce.access.service.EffectivePermissionService;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Effective Permissions", description = "Permissões efetivas e verificação de acesso")
public class EffectivePermissionController {

    private final EffectivePermissionService effectivePermissionService;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;

    @GetMapping("/me/permissions")
    @Operation(summary = "Minhas permissões efetivas")
    public ResponseEntity<ApiResponse<EffectivePermissionsResponse>> mine(
            @RequestParam(defaultValue = "false") boolean includeOrigins) {
        return ResponseEntity.ok(ApiResponse.of(effectivePermissionService.mine(includeOrigins)));
    }

    @GetMapping("/me/access-version")
    @Operation(summary = "Versão de acesso da sessão")
    public ResponseEntity<ApiResponse<AccessVersionResponse>> accessVersion() {
        UUID userId = CurrentUser.requireId();
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        long av = user.getAccessVersion() == null ? 0L : user.getAccessVersion();
        return ResponseEntity.ok(ApiResponse.of(new AccessVersionResponse(userId, av)));
    }

    @GetMapping("/me/menus")
    @Operation(summary = "Menus autorizados conforme permissões efetivas")
    public ResponseEntity<ApiResponse<List<AuthorizedMenuItem>>> menus() {
        Set<String> codes = effectivePermissionService.mine(false).permissions().stream()
                .map(p -> p.code())
                .collect(Collectors.toSet());
        List<AuthorizedMenuItem> catalog = menuCatalog();
        List<AuthorizedMenuItem> allowed = catalog.stream()
                .filter(m -> m.requiredPermissions().isEmpty()
                        || m.requiredPermissions().stream().anyMatch(codes::contains))
                .toList();
        return ResponseEntity.ok(ApiResponse.of(allowed));
    }

    @GetMapping("/users/{userId}/effective-access")
    @PreAuthorize(
            "hasAuthority('EFFECTIVE_PERMISSION_READ') or hasAuthority('ACCESS_GROUP_MEMBER_MANAGE') or hasAuthority('USER_READ')")
    @Operation(summary = "Permissões efetivas de um usuário (com origem)")
    public ResponseEntity<ApiResponse<EffectivePermissionsResponse>> forUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(effectivePermissionService.forUser(userId, true)));
    }

    @GetMapping("/users/{userId}/permissions/{code}/explain")
    @PreAuthorize("hasAuthority('EFFECTIVE_PERMISSION_READ') or hasAuthority('ACCESS_GROUP_MEMBER_MANAGE')")
    @Operation(summary = "Explica origem de uma permissão")
    public ResponseEntity<ApiResponse<PermissionExplainResponse>> explain(
            @PathVariable UUID userId, @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.of(effectivePermissionService.explain(userId, code)));
    }

    @PostMapping("/authorization/check")
    @Operation(summary = "Verifica acesso a uma ação/recurso")
    public ResponseEntity<ApiResponse<AccessCheckResponse>> check(@Valid @RequestBody AccessCheckRequest request) {
        boolean allowed;
        String reason;
        if (request.resourceType() != null && request.resourceId() != null) {
            allowed = authorizationService.canAccessResource(
                    request.permissionCode(), request.resourceType(), request.resourceId());
            reason = allowed ? "OK" : "Recurso ou escopo negado";
        } else if (request.storeId() != null) {
            allowed = authorizationService.hasStorePermission(request.permissionCode(), request.storeId());
            reason = allowed ? "OK" : "Loja ou permissão negada";
        } else {
            allowed = authorizationService.hasPermission(request.permissionCode());
            reason = allowed ? "OK" : "Permissão ausente";
        }
        return ResponseEntity.ok(ApiResponse.of(new AccessCheckResponse(allowed, reason)));
    }

    private static List<AuthorizedMenuItem> menuCatalog() {
        List<AuthorizedMenuItem> items = new ArrayList<>();
        items.add(new AuthorizedMenuItem(
                "Geral",
                "Início",
                "/dashboard",
                "dashboard",
                10,
                List.of("DASHBOARD_READ", "DASHBOARD_STORE_READ"),
                "STORE",
                List.of("READ")));
        items.add(new AuthorizedMenuItem(
                "Administração",
                "Usuários",
                "/administracao/usuarios",
                "users",
                20,
                List.of("USER_READ"),
                "ORGANIZATION",
                List.of("READ", "CREATE", "UPDATE")));
        items.add(new AuthorizedMenuItem(
                "Administração",
                "Grupos",
                "/administracao/grupos",
                "shield",
                30,
                List.of("ACCESS_GROUP_READ", "ROLE_READ"),
                "ORGANIZATION",
                List.of("READ", "CREATE", "UPDATE")));
        items.add(new AuthorizedMenuItem(
                "Administração",
                "Auditoria de acesso",
                "/administracao/auditoria-acesso",
                "eye",
                40,
                List.of("ACCESS_AUDIT_READ", "AUDIT_READ"),
                "ORGANIZATION",
                List.of("READ")));
        items.add(new AuthorizedMenuItem(
                "Administração",
                "Catálogo de permissões",
                "/administracao/catalogo-permissoes",
                "list",
                50,
                List.of("ACCESS_CATALOG_READ"),
                "ORGANIZATION",
                List.of("READ")));
        items.add(new AuthorizedMenuItem(
                "Vendas",
                "Pedidos",
                "/sales-orders",
                "cart",
                60,
                List.of("SALES_ORDER_READ"),
                "STORE",
                List.of("READ", "CREATE", "CANCEL")));
        items.add(new AuthorizedMenuItem(
                "Cadastros",
                "Clientes",
                "/customers",
                "users",
                70,
                List.of("CUSTOMER_READ"),
                "STORE",
                List.of("READ", "CREATE")));
        return items;
    }
}
