package br.com.systemcommerce.salesorder.controller;

import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderCreateRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderStatusHistoryResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderUpdateRequest;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
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
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Sales Orders", description = "Pedidos de venda (compromisso comercial; sem baixa na criação)")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('SALES_ORDER_READ')")
    @Operation(summary = "Lista pedidos de venda")
    public ResponseEntity<PageResponse<SalesOrderResponse>> list(
            @RequestParam(required = false) SalesOrder.SalesOrderStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(salesOrderService.list(status, storeId, customerId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALES_ORDER_READ')")
    @Operation(summary = "Consulta pedido por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SalesOrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('SALES_ORDER_READ')")
    @Operation(summary = "Histórico de status do pedido")
    public ResponseEntity<ApiResponse<List<SalesOrderStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALES_ORDER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria pedido em DRAFT com itens")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> create(
            @Valid @RequestBody SalesOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(salesOrderService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Atualiza pedido (DRAFT / PENDING_APPROVAL)")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SalesOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.update(id, request)));
    }

    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Envia pedido para aprovação")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> submitForApproval(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.submitForApproval(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Aprova pedido")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.approve(id)));
    }

    @PostMapping("/{id}/start-picking")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Inicia separação")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> startPicking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.startPicking(id)));
    }

    @PostMapping("/{id}/mark-picked")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Marca pedido como separado")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> markPicked(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.markPicked(id)));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('SALES_ORDER_BILL','SALES_ORDER_UPDATE')")
    @Operation(
            summary = "Fatura pedido (efetiva venda)",
            description =
                    "Prompt 59: em uma transação cria/confirma a Sale (baixa estoque + movimentações), "
                            + "marca pedido INVOICED e grava histórico de faturamento. Rollback completo em erro.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> invoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.invoice(id)));
    }

    @GetMapping("/{id}/billing-history")
    @PreAuthorize("hasAuthority('SALES_ORDER_READ')")
    @Operation(summary = "Histórico de faturamento do pedido")
    public ResponseEntity<ApiResponse<java.util.List<br.com.systemcommerce.salesorder.dto.SalesOrderBillingHistoryResponse>>>
            billingHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.billingHistory(id)));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    @Operation(summary = "Marca pedido como entregue")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> deliver(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.deliver(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SALES_ORDER_CANCEL')")
    @Operation(summary = "Cancela pedido")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.cancel(id, notes)));
    }

    @PostMapping("/{id}/generate-sale")
    @PreAuthorize("hasAuthority('SALES_ORDER_GENERATE_SALE')")
    @Operation(summary = "Gera Sale DRAFT a partir do pedido (PICKED/INVOICED)")
    public ResponseEntity<ApiResponse<SaleResponse>> generateSale(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(salesOrderService.generateSale(id)));
    }
}
