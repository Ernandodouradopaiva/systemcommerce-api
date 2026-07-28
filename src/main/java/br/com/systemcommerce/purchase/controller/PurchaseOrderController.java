package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.PurchaseOrderCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderUpdateRequest;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.service.PurchaseOrderService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Purchase Orders", description = "Pedidos de compra (Prompt 60) — fluxo até recebimento")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ')")
    @Operation(summary = "Lista pedidos de compra")
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> list(
            @RequestParam(required = false) PurchaseOrder.PurchaseOrderStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(purchaseOrderService.list(status, storeId, supplierId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ')")
    @Operation(summary = "Consulta pedido de compra por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.getById(id)));
    }

    @GetMapping("/{id}/print-data")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ')")
    @Operation(summary = "Dados para impressão do pedido de compra")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> printData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.printData(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ')")
    @Operation(summary = "Histórico de status do pedido de compra")
    public ResponseEntity<ApiResponse<List<PurchaseOrderStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria pedido de compra em DRAFT")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(
            @Valid @RequestBody PurchaseOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(purchaseOrderService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_UPDATE')")
    @Operation(summary = "Atualiza pedido de compra (somente DRAFT)")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.update(id, request)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_UPDATE')")
    @Operation(summary = "Envia pedido (DRAFT → SENT)")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> send(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.send(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_UPDATE')")
    @Operation(summary = "Aprova pedido (SENT → APPROVED)")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.approve(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CANCEL')")
    @Operation(summary = "Cancela pedido de compra")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(purchaseOrderService.cancel(id, notes)));
    }
}
