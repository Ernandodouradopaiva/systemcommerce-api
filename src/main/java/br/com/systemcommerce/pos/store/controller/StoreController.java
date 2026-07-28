package br.com.systemcommerce.pos.store.controller;

import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.dto.StoreResponse;
import br.com.systemcommerce.pos.store.dto.StoreSummaryResponse;
import br.com.systemcommerce.pos.store.dto.StoreUpdateRequest;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Stores", description = "Cadastro de lojas / estabelecimentos")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_READ') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Lista lojas paginadas")
    public ResponseEntity<PageResponse<StoreResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Store.StoreStatus status,
            @RequestParam(required = false) Store.EstablishmentType establishmentType,
            @RequestParam(required = false) Boolean headquarters,
            @RequestParam(required = false) Boolean allowsSales,
            @RequestParam(required = false) Boolean allowsPos,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(storeService.list(
                organizationId,
                code,
                status,
                establishmentType,
                headquarters,
                allowsSales,
                allowsPos,
                search,
                pageable)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('STORE_READ') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Pesquisa lojas por texto")
    public ResponseEntity<PageResponse<StoreResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(storeService.search(q, organizationId, pageable)));
    }

    @GetMapping("/operational")
    @PreAuthorize("hasAuthority('STORE_READ') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Lista lojas operacionais (ativas e aptas a venda e/ou PDV)")
    public ResponseEntity<PageResponse<StoreResponse>> listOperational(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(storeService.listOperational(organizationId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_READ') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Consulta loja por ID")
    public ResponseEntity<ApiResponse<StoreResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeService.getById(id)));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('STORE_READ') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Consulta resumo operacional da loja")
    public ResponseEntity<ApiResponse<StoreSummaryResponse>> getSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeService.getSummary(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STORE_CREATE') or hasAuthority('STORE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra loja")
    public ResponseEntity<ApiResponse<StoreResponse>> create(@Valid @RequestBody StoreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(storeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_UPDATE') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Atualiza loja")
    public ResponseEntity<ApiResponse<StoreResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody StoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('STORE_ACTIVATE') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Ativa loja")
    public ResponseEntity<ApiResponse<StoreResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('STORE_DEACTIVATE') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Inativa loja (bloqueia se houver caixa aberto ou transferência pendente)")
    public ResponseEntity<ApiResponse<StoreResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeService.inactivate(id)));
    }

    @PatchMapping("/{id}/headquarters")
    @PreAuthorize("hasAuthority('STORE_UPDATE') or hasAuthority('STORE_MANAGE')")
    @Operation(summary = "Define loja como matriz da organização")
    public ResponseEntity<ApiResponse<StoreResponse>> defineHeadquarters(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeService.defineHeadquarters(id)));
    }
}
