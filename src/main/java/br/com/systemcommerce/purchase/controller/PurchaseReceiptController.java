package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.GoodsReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptInspectionRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptStatusHistoryResponse;
import br.com.systemcommerce.purchase.service.PurchaseReceiptService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

@RestController
@RequestMapping("/api/v1/purchase-receipts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "Purchase Receipts",
        description = "GoodsReceipt (Prompt 62) — fluxo draft → inspeção → aceite → postagem em estoque")
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_READ')")
    @Operation(summary = "Lista recebimentos de compra")
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
    @Operation(summary = "Consulta recebimento por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_READ')")
    @Operation(summary = "Histórico de status do recebimento")
    public ResponseEntity<ApiResponse<List<PurchaseReceiptStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria recebimento em DRAFT (não movimenta estoque)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> createDraft(
            @Valid @RequestBody GoodsReceiptCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(purchaseReceiptService.createDraft(request)));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Atalho legado: cria + aceita + posta em estoque em uma chamada")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> createAndConfirm(
            @Valid @RequestBody PurchaseReceiptCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(purchaseReceiptService.createAndConfirm(request)));
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Registra inspeção (quantidade aceita e divergências) — ainda sem tocar estoque")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> inspect(
            @PathVariable UUID id, @Valid @RequestBody GoodsReceiptInspectionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.inspect(id, request)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Confirma aceite total ou parcial do recebimento")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.accept(id)));
    }

    @PostMapping("/{id}/post-to-inventory")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_POST')")
    @Operation(
            summary = "Posta a quantidade aceita no estoque (idempotente via header Idempotency-Key)",
            description = "Cria movimentação PURCHASE oficial via InventoryService; atualiza saldo recebido do pedido")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> postToInventory(
            @PathVariable UUID id,
            @Parameter(description = "Chave de idempotência para evitar postagem duplicada")
                    @RequestHeader(name = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.postToInventory(id, idempotencyKey)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Rejeita o recebimento (motivo obrigatório)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> reject(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Motivo da rejeição é obrigatório");
        }
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.reject(id, reason)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIPT_CREATE')")
    @Operation(summary = "Cancela o recebimento (somente antes da postagem em estoque)")
    public ResponseEntity<ApiResponse<PurchaseReceiptResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.of(purchaseReceiptService.cancel(id, reason)));
    }
}
