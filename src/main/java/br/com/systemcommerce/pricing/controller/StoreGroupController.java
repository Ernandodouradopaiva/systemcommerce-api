package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.StoreGroupCreateRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupResponse;
import br.com.systemcommerce.pricing.dto.StoreGroupStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupUpdateRequest;
import br.com.systemcommerce.pricing.service.StoreGroupService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/store-groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Store Groups", description = "Grupos de lojas para precificação")
public class StoreGroupController {

    private final StoreGroupService storeGroupService;

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_GROUP_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista grupos de lojas")
    public ResponseEntity<PageResponse<StoreGroupResponse>> list(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(storeGroupService.list(organizationId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_GROUP_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Consulta grupo de lojas por ID")
    public ResponseEntity<ApiResponse<StoreGroupResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeGroupService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STORE_GROUP_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria grupo de lojas")
    public ResponseEntity<ApiResponse<StoreGroupResponse>> create(@Valid @RequestBody StoreGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(storeGroupService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_GROUP_MANAGE')")
    @Operation(summary = "Atualiza grupo de lojas")
    public ResponseEntity<ApiResponse<StoreGroupResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody StoreGroupUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeGroupService.update(id, request)));
    }

    @PostMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('STORE_GROUP_MANAGE')")
    @Operation(summary = "Vincula loja ao grupo")
    public ResponseEntity<ApiResponse<StoreGroupResponse>> linkStore(
            @PathVariable UUID id, @Valid @RequestBody StoreGroupStoreLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeGroupService.linkStore(id, request)));
    }
}
