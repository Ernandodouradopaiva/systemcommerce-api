package br.com.systemcommerce.pos.cancellation.controller;

import br.com.systemcommerce.pos.cancellation.dto.CancellationDecisionRequest;
import br.com.systemcommerce.pos.cancellation.dto.CancellationRequestCreate;
import br.com.systemcommerce.pos.cancellation.dto.SaleCancellationResponse;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.service.PosCancellationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/cancellations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Cancellations",
        description =
                "Cancelamentos do PDV: solicitação, autorização, execução, estornos com falha controlada e reprocessamento.")
public class PosCancellationController {

    private final PosCancellationService posCancellationService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('POS_CANCEL_DRAFT') or hasAuthority('POS_CANCEL_COMPLETED_SALE') or hasAuthority('POS_SALE_CANCEL') or hasAuthority('POS_CANCEL_AUTHORIZE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita cancelamento (rascunho executa na hora)")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> request(
            @Valid @RequestBody CancellationRequestCreate request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(posCancellationService.request(request, idempotencyKey)));
    }

    @PostMapping("/{id}/authorize")
    @PreAuthorize("hasAuthority('POS_CANCEL_AUTHORIZE')")
    @Operation(summary = "Autoriza cancelamento de venda concluída")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> authorize(
            @PathVariable UUID id,
            @RequestBody(required = false) CancellationDecisionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posCancellationService.authorize(id, request)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('POS_CANCEL_AUTHORIZE')")
    @Operation(summary = "Rejeita solicitação de cancelamento")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> reject(
            @PathVariable UUID id, @RequestBody(required = false) CancellationDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posCancellationService.reject(id, request)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize(
            "hasAuthority('POS_CANCEL_DRAFT') or hasAuthority('POS_CANCEL_COMPLETED_SALE') or hasAuthority('POS_SALE_CANCEL')")
    @Operation(summary = "Executa cancelamento autorizado (estoque + estornos)")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> execute(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posCancellationService.execute(id)));
    }

    @PostMapping("/{id}/refunds/{refundId}/reprocess")
    @PreAuthorize("hasAuthority('POS_REFUND_EXECUTE') or hasAuthority('POS_PAYMENT_REFUND')")
    @Operation(summary = "Reprocessa estorno com falha")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> reprocessRefund(
            @PathVariable UUID id, @PathVariable UUID refundId) {
        return ResponseEntity.ok(ApiResponse.of(posCancellationService.reprocessRefund(id, refundId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('POS_CANCEL_DRAFT') or hasAuthority('POS_CANCEL_COMPLETED_SALE') or hasAuthority('POS_CANCEL_AUTHORIZE') or hasAuthority('POS_SALE_CANCEL') or hasAuthority('POS_REFUND_EXECUTE')")
    @Operation(summary = "Consulta andamento do cancelamento")
    public ResponseEntity<ApiResponse<SaleCancellationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posCancellationService.getById(id)));
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('POS_CANCEL_DRAFT') or hasAuthority('POS_CANCEL_COMPLETED_SALE') or hasAuthority('POS_CANCEL_AUTHORIZE') or hasAuthority('POS_SALE_CANCEL') or hasAuthority('POS_REFUND_EXECUTE')")
    @Operation(summary = "Lista cancelamentos")
    public ResponseEntity<PageResponse<SaleCancellationResponse>> list(
            @RequestParam(required = false) UUID saleId,
            @RequestParam(required = false) SaleCancellation.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posCancellationService.list(saleId, status, pageable)));
    }
}
