package br.com.systemcommerce.stockentry.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.stockentry.dto.StockEntryCreateRequest;
import br.com.systemcommerce.stockentry.dto.StockEntryItemCreateRequest;
import br.com.systemcommerce.stockentry.dto.StockEntryResponse;
import br.com.systemcommerce.stockentry.entity.StockEntryStatus;
import br.com.systemcommerce.stockentry.service.StockEntryService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-entries")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Stock Entries", description = "Entradas oficiais de estoque por loja/depósito")
public class StockEntryController {

    private final StockEntryService stockEntryService;

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_ENTRY_READ') or hasAuthority('STOCK_ENTRY_MANAGE')")
    @Operation(summary = "Lista entradas de estoque")
    public ResponseEntity<PageResponse<StockEntryResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) StockEntryStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(stockEntryService.list(
                organizationId, storeId, warehouseId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_ENTRY_READ') or hasAuthority('STOCK_ENTRY_MANAGE')")
    @Operation(summary = "Consulta entrada por ID")
    public ResponseEntity<ApiResponse<StockEntryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockEntryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_ENTRY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria entrada em rascunho")
    public ResponseEntity<ApiResponse<StockEntryResponse>> create(@Valid @RequestBody StockEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(stockEntryService.create(request)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('STOCK_ENTRY_MANAGE')")
    @Operation(summary = "Adiciona item à entrada (rascunho)")
    public ResponseEntity<ApiResponse<StockEntryResponse>> addItem(
            @PathVariable UUID id, @Valid @RequestBody StockEntryItemCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockEntryService.addItem(id, request)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('STOCK_ENTRY_MANAGE')")
    @Operation(summary = "Confirma entrada e registra movimentos de estoque")
    public ResponseEntity<ApiResponse<StockEntryResponse>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockEntryService.confirm(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('STOCK_ENTRY_MANAGE')")
    @Operation(summary = "Cancela entrada em rascunho")
    public ResponseEntity<ApiResponse<StockEntryResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockEntryService.cancel(id)));
    }
}
