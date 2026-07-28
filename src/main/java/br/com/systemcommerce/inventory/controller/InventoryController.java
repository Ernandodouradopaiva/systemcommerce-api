package br.com.systemcommerce.inventory.controller;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentReasonResponse;
import br.com.systemcommerce.inventory.dto.InventoryAdjustmentRequest;
import br.com.systemcommerce.inventory.dto.InventoryAvailabilityResponse;
import br.com.systemcommerce.inventory.dto.InventoryBalanceResponse;
import br.com.systemcommerce.inventory.dto.InventoryConsolidatedBalanceResponse;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.dto.InventoryExitRequest;
import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Inventory", description = "Estoque multilojas/multidepósito (saldos e fórmulas oficiais na API)")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Lista saldos", description = "Filtros: productId, storeId, warehouseId, search, belowMinimum")
    public ResponseEntity<PageResponse<InventoryBalanceResponse>> list(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean belowMinimum,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.list(productId, storeId, warehouseId, search, belowMinimum, pageable)));
    }

    @GetMapping("/by-store/{storeId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Saldo por loja (soma dos depósitos da loja, listagem por depósito)")
    public ResponseEntity<PageResponse<InventoryBalanceResponse>> byStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) Boolean belowMinimum,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.list(productId, storeId, null, null, belowMinimum, pageable)));
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Saldo por depósito")
    public ResponseEntity<PageResponse<InventoryBalanceResponse>> byWarehouse(
            @PathVariable UUID warehouseId,
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.list(productId, null, warehouseId, null, null, pageable)));
    }

    @GetMapping("/below-minimum")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Produtos abaixo do mínimo (por depósito)")
    public ResponseEntity<PageResponse<InventoryBalanceResponse>> belowMinimum(
            @RequestParam(required = false) UUID storeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.list(null, storeId, null, null, true, pageable)));
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Saldo de um produto em um depósito", description = "warehouseId opcional; default = DEP-01")
    public ResponseEntity<ApiResponse<InventoryBalanceResponse>> balance(
            @PathVariable UUID productId, @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.getBalance(productId, warehouseId)));
    }

    @GetMapping("/products/{productId}/consolidated")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('STORE_CONSOLIDATED_READ')")
    @Operation(summary = "Saldo consolidado do produto (soma no backend)")
    public ResponseEntity<ApiResponse<InventoryConsolidatedBalanceResponse>> consolidated(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.getConsolidatedBalance(productId)));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Consulta disponibilidade para venda no depósito")
    public ResponseEntity<ApiResponse<InventoryAvailabilityResponse>> availability(
            @RequestParam UUID productId, @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.checkAvailability(productId, warehouseId)));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    @Operation(summary = "Lista movimentações", description = "Filtros: productId, storeId, warehouseId, type, from, to")
    public ResponseEntity<PageResponse<InventoryMovementResponse>> movements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) InventoryMovement.MovementType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.listMovements(productId, storeId, warehouseId, type, from, to, pageable)));
    }

    @GetMapping("/adjustment-reasons")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_ADJUST')")
    @Operation(summary = "Lista motivos de ajuste ativos")
    public ResponseEntity<ApiResponse<List<InventoryAdjustmentReasonResponse>>> reasons() {
        return ResponseEntity.ok(ApiResponse.of(inventoryService.listActiveReasons()));
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('INVENTORY_MOVE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Entrada de estoque por depósito")
    public ResponseEntity<ApiResponse<InventoryMovementResponse>> entry(
            @Valid @RequestBody InventoryEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(inventoryService.registerEntry(request)));
    }

    @PostMapping("/exits")
    @PreAuthorize("hasAuthority('INVENTORY_MOVE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Saída manual de estoque por depósito")
    public ResponseEntity<ApiResponse<InventoryMovementResponse>> exit(
            @Valid @RequestBody InventoryExitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(inventoryService.registerExit(request)));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajuste de estoque por depósito (não simula transferência)")
    public ResponseEntity<ApiResponse<InventoryMovementResponse>> adjustment(
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(inventoryService.registerAdjustment(request)));
    }
}
