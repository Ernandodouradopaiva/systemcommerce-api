package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.GoodsReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptInspectionRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptStatusHistoryResponse;
import br.com.systemcommerce.purchase.service.PurchaseReceiptService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alias de negócio "GoodsReceipt" para {@code /api/v1/purchase-receipts} (Prompt 62).
 * Mesma entidade/tabela canônica; apenas expõe a nomenclatura usada por integrações externas.
 */
@RestController
@RequestMapping("/api/v1/goods-receipts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Goods Receipts", description = "Alias de GoodsReceipt para Purchase Receipts (mesmo recurso)")
public class GoodsReceiptAliasController {

    private final PurchaseReceiptService purchaseReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_READ')")
    @Operation(summary = "Lista recebimentos (alias GoodsReceipt)")
    public ResponseEntity<PageResponse<PurchaseReceiptResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID purchaseOrderId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                purchaseReceiptService.list(storeId, purchaseOrderId, supplierId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_READ')")
    @Operation(summary = "Consulta recebimento por ID (alias GoodsReceipt)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_READ')")
    @Operation(summary = "Histórico de status (alias GoodsReceipt)")
    public ResponseEntity<ApiResponse<List<PurchaseReceiptStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria GoodsReceipt em DRAFT")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> createDraft(
            @Valid @RequestBody GoodsReceiptCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(purchaseReceiptService.createDraft(request)));
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Registra inspeção (alias GoodsReceipt)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> inspect(
            @PathVariable UUID id, @Valid @RequestBody GoodsReceiptInspectionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.inspect(id, request)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Confirma aceite (alias GoodsReceipt)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.accept(id)));
    }

    @PostMapping("/{id}/post-to-inventory")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_POST')")
    @Operation(summary = "Posta em estoque (alias GoodsReceipt, idempotente via Idempotency-Key)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> postToInventory(
            @PathVariable UUID id,
            @Parameter(description = "Chave de idempotência") @RequestHeader(name = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.postToInventory(id, idempotencyKey)));
    }
}
