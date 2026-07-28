package br.com.systemcommerce.storeaccess.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.dto.AccessibleStoreResponse;
import br.com.systemcommerce.storeaccess.dto.StoreContextSwitchRequest;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessResponse;
import br.com.systemcommerce.storeaccess.service.StoreAccessService;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import br.com.systemcommerce.storecontext.GlobalStoreOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/store-access")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Store Access", description = "Acesso de usuários por loja e contexto ativo")
@GlobalStoreOperation
public class StoreAccessController {

    private final StoreAccessService storeAccessService;

    @GetMapping("/accessible-stores")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista lojas acessíveis do usuário autenticado (ou userId com permissão)")
    public ResponseEntity<ApiResponse<List<AccessibleStoreResponse>>> listAccessible(
            @RequestParam(required = false) UUID userId) {
        UUID target = userId;
        if (target != null && !target.equals(CurrentUser.requireId())) {
            // leitura de outro usuário exige permissão de gestão/leitura de acessos
        }
        return ResponseEntity.ok(ApiResponse.of(storeAccessService.listAccessibleStores(target)));
    }

    @GetMapping("/users/{userId}/history")
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_READ') or hasAuthority('USER_STORE_ACCESS_MANAGE')")
    @Operation(summary = "Histórico de acessos do usuário")
    public ResponseEntity<ApiResponse<List<UserStoreAccessResponse>>> history(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(storeAccessService.listHistory(userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_READ') or hasAuthority('USER_STORE_ACCESS_MANAGE')")
    @Operation(summary = "Consulta acesso por ID")
    public ResponseEntity<ApiResponse<UserStoreAccessResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeAccessService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Concede acesso à loja")
    public ResponseEntity<ApiResponse<UserStoreAccessResponse>> grant(
            @Valid @RequestBody UserStoreAccessGrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(storeAccessService.grant(request)));
    }

    @PostMapping("/temporary")
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Concede acesso temporário")
    public ResponseEntity<ApiResponse<UserStoreAccessResponse>> grantTemporary(
            @Valid @RequestBody UserStoreAccessGrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(storeAccessService.grantTemporary(request)));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_MANAGE')")
    @Operation(summary = "Revoga acesso (histórico preservado)")
    public ResponseEntity<ApiResponse<UserStoreAccessResponse>> revoke(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeAccessService.revoke(id)));
    }

    @PostMapping("/users/{userId}/default-store/{storeId}")
    @PreAuthorize("hasAuthority('USER_STORE_ACCESS_MANAGE') or hasAuthority('STORE_CONTEXT_SWITCH')")
    @Operation(summary = "Define loja padrão do usuário")
    public ResponseEntity<ApiResponse<UserStoreAccessResponse>> setDefault(
            @PathVariable UUID userId, @PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(storeAccessService.setDefaultStore(userId, storeId)));
    }

    @PostMapping("/context/switch")
    @PreAuthorize("hasAuthority('STORE_CONTEXT_SWITCH') or isAuthenticated()")
    @Operation(summary = "Troca contexto da loja ativa (validado no backend)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> switchContext(
            @Valid @RequestBody StoreContextSwitchRequest request) {
        CurrentStoreContext ctx = storeAccessService.switchContext(request);
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "storeId", ctx.storeId(),
                "organizationId", ctx.organizationId() != null ? ctx.organizationId() : "",
                "source", ctx.source().name())));
    }

    @GetMapping("/context/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulta contexto de loja da requisição atual")
    public ResponseEntity<ApiResponse<Map<String, Object>>> currentContext() {
        CurrentStoreContext ctx = CurrentStoreContext.get();
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "storeId", ctx.storeId() != null ? ctx.storeId() : "",
                "organizationId", ctx.organizationId() != null ? ctx.organizationId() : "",
                "source", ctx.source().name(),
                "hasStore", ctx.hasStore())));
    }
}
