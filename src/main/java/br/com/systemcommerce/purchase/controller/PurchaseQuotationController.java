package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.GeneratePurchaseOrdersRequest;
import br.com.systemcommerce.purchase.dto.InviteSuppliersRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.QuotationComparisonResponse;
import br.com.systemcommerce.purchase.dto.SelectQuotationItemsRequest;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import br.com.systemcommerce.purchase.service.PurchaseQuotationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-quotations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Purchase Quotations", description = "Cotação de compra multifornecedor (Prompt 60)")
public class PurchaseQuotationController {

    private final PurchaseQuotationService purchaseQuotationService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_READ')")
    @Operation(summary = "Lista cotações de compra")
    public ResponseEntity<PageResponse<PurchaseQuotationResponse>> list(
            @RequestParam(required = false) PurchaseQuotation.PurchaseQuotationStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(purchaseQuotationService.list(status, storeId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_READ')")
    @Operation(summary = "Consulta cotação por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.getById(id)));
    }

    @GetMapping("/{id}/print-data")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_READ')")
    @Operation(summary = "Dados para impressão da cotação")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> printData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.printData(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_READ')")
    @Operation(summary = "Histórico de status da cotação")
    public ResponseEntity<ApiResponse<List<PurchaseQuotationStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.statusHistory(id)));
    }

    @GetMapping("/{id}/comparison")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_READ')")
    @Operation(summary = "Mapa comparativo de respostas por item/fornecedor")
    public ResponseEntity<ApiResponse<QuotationComparisonResponse>> comparison(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.comparison(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria cotação de compra (manual)")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> create(
            @Valid @RequestBody PurchaseQuotationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(purchaseQuotationService.create(request)));
    }

    @PostMapping("/{id}/invite-suppliers")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_UPDATE')")
    @Operation(summary = "Convida fornecedores para a cotação")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> inviteSuppliers(
            @PathVariable UUID id, @Valid @RequestBody InviteSuppliersRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.inviteSuppliers(id, request)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_UPDATE')")
    @Operation(summary = "Envia/abre a cotação para os fornecedores convidados")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> send(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.send(id)));
    }

    @PostMapping("/{id}/suppliers/{supplierId}/register-response")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_UPDATE')")
    @Operation(summary = "Registra a resposta de um fornecedor convidado")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> registerResponse(
            @PathVariable UUID id,
            @PathVariable UUID supplierId,
            @Valid @RequestBody SupplierQuotationResponseRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.registerResponse(id, supplierId, request)));
    }

    @PostMapping("/{id}/select-items")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_UPDATE')")
    @Operation(summary = "Seleciona itens/fornecedores vencedores (parcial ou total)")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> selectItems(
            @PathVariable UUID id, @Valid @RequestBody SelectQuotationItemsRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.selectItems(id, request)));
    }

    @PostMapping("/{id}/generate-purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_GENERATE_PO')")
    @Operation(summary = "Gera um pedido de compra por fornecedor selecionado")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> generatePurchaseOrders(
            @PathVariable UUID id, @Valid @RequestBody GeneratePurchaseOrdersRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.generatePurchaseOrders(id, request)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_UPDATE')")
    @Operation(summary = "Encerra a cotação (trava respostas)")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.close(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_QUOTATION_CANCEL')")
    @Operation(summary = "Cancela a cotação (trava respostas)")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.of(purchaseQuotationService.cancel(id, reason)));
    }
}
