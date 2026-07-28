package br.com.systemcommerce.pos.warehouse.controller;

import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseUpdateRequest;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
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
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Warehouses", description = "Cadastro de depósitos / locais de estoque (PDV)")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_READ') or hasAuthority('WAREHOUSE_MANAGE')")
    @Operation(summary = "Lista depósitos paginados")
    public ResponseEntity<PageResponse<WarehouseResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Warehouse.WarehouseStatus status,
            @RequestParam(required = false) Boolean allowsSale,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(warehouseService.list(storeId, status, allowsSale, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_READ') or hasAuthority('WAREHOUSE_MANAGE')")
    @Operation(summary = "Consulta depósito por ID")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(warehouseService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra depósito")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(warehouseService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    @Operation(summary = "Edita depósito")
    public ResponseEntity<ApiResponse<WarehouseResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody WarehouseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(warehouseService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    @Operation(summary = "Ativa depósito")
    public ResponseEntity<ApiResponse<WarehouseResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(warehouseService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('WAREHOUSE_MANAGE')")
    @Operation(summary = "Inativa depósito")
    public ResponseEntity<ApiResponse<WarehouseResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(warehouseService.inactivate(id)));
    }
}
