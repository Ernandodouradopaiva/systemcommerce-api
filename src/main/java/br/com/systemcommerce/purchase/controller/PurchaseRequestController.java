package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestConvertRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestPartialApprovalRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestUpdateRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import br.com.systemcommerce.purchase.service.PurchaseRequestService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Purchase Requests", description = "Solicitações internas de compra (Prompt 59)")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_READ')")
    @Operation(summary = "Lista solicitações de compra")
    public ResponseEntity<PageResponse<PurchaseRequestResponse>> list(
            @RequestParam(required = false) PurchaseRequest.PurchaseRequestStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(purchaseRequestService.list(status, storeId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_READ')")
    @Operation(summary = "Consulta solicitação de compra por ID")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.getById(id)));
    }

    @GetMapping("/{id}/print-data")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_READ')")
    @Operation(summary = "Dados para impressão da solicitação")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> printData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.printData(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_READ')")
    @Operation(summary = "Histórico de status da solicitação")
    public ResponseEntity<ApiResponse<List<PurchaseRequestStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria solicitação de compra em DRAFT")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> create(
            @Valid @RequestBody PurchaseRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(purchaseRequestService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_UPDATE')")
    @Operation(summary = "Atualiza solicitação de compra (somente DRAFT)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PurchaseRequestUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_UPDATE')")
    @Operation(summary = "Envia solicitação para análise (DRAFT → SUBMITTED)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.submit(id)));
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_UPDATE')")
    @Operation(summary = "Inicia análise (SUBMITTED → UNDER_ANALYSIS)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> analyze(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.analyze(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_APPROVE')")
    @Operation(summary = "Aprova integralmente a solicitação")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.approve(id)));
    }

    @PostMapping("/{id}/partially-approve")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_APPROVE')")
    @Operation(summary = "Aprova parcialmente a solicitação (quantidades por item)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> partiallyApprove(
            @PathVariable UUID id, @Valid @RequestBody PurchaseRequestPartialApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.partiallyApprove(id, request)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_APPROVE')")
    @Operation(summary = "Rejeita a solicitação (motivo obrigatório)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> reject(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.reject(id, body.get("reason"))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_CANCEL')")
    @Operation(summary = "Cancela a solicitação (motivo obrigatório)")
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> cancel(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.cancel(id, body.get("reason"))));
    }

    @PostMapping("/{id}/convert-to-quotation")
    @PreAuthorize("hasAuthority('PURCHASE_REQUEST_CONVERT')")
    @Operation(summary = "Converte o saldo pendente em uma cotação de compra")
    public ResponseEntity<ApiResponse<PurchaseQuotationResponse>> convertToQuotation(
            @PathVariable UUID id, @Valid @RequestBody(required = false) PurchaseRequestConvertRequest request) {
        PurchaseRequestConvertRequest body = request != null ? request : new PurchaseRequestConvertRequest(
                null, null, null, null);
        return ResponseEntity.ok(ApiResponse.of(purchaseRequestService.convertToQuotation(id, body)));
    }
}
