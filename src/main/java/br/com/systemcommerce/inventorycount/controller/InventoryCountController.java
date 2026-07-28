package br.com.systemcommerce.inventorycount.controller;

import br.com.systemcommerce.inventorycount.dto.InventoryCountActionRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountCreateRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountEntryRequest;
import br.com.systemcommerce.inventorycount.dto.InventoryCountResponse;
import br.com.systemcommerce.inventorycount.dto.InventoryCountStatusHistoryResponse;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import br.com.systemcommerce.inventorycount.service.InventoryCountService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory-counts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Inventory Counts", description = "Inventário físico e rotativo (Prompt 74)")
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_READ')")
    @Operation(summary = "Lista inventários")
    public ResponseEntity<PageResponse<InventoryCountResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) InventoryCountStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(inventoryCountService.list(storeId, warehouseId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_READ')")
    @Operation(summary = "Consulta inventário por ID")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_READ')")
    @Operation(summary = "Histórico de status")
    public ResponseEntity<ApiResponse<List<InventoryCountStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria inventário planejado")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> create(
            @Valid @RequestBody InventoryCountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(inventoryCountService.create(request)));
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Abre inventário")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> open(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.open(id, request)));
    }

    @PostMapping("/{id}/start-counting")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Inicia sessão de contagem")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> startCounting(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.startCounting(id, request)));
    }

    @PostMapping("/{id}/entries")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Registra entrada de contagem")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> recordEntry(
            @PathVariable UUID id, @Valid @RequestBody InventoryCountEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.recordEntry(id, request)));
    }

    @PostMapping("/{id}/submit-for-analysis")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Envia para análise")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> submitForAnalysis(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.submitForAnalysis(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Aprova inventário")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> approve(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.approve(id, request)));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_POST')")
    @Operation(summary = "Posta ajustes de inventário")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> post(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.post(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_MANAGE')")
    @Operation(summary = "Cancela inventário")
    public ResponseEntity<ApiResponse<InventoryCountResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) InventoryCountActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(inventoryCountService.cancel(id, request)));
    }
}
