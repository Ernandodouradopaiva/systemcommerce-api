package br.com.systemcommerce.pos.cash.controller;

import br.com.systemcommerce.pos.cash.dto.CashClosingReceiptResponse;
import br.com.systemcommerce.pos.cash.dto.CashConferenceRequest;
import br.com.systemcommerce.pos.cash.dto.CashConferenceResponse;
import br.com.systemcommerce.pos.cash.dto.CashReconciliationResponse;
import br.com.systemcommerce.pos.cash.dto.CashSessionCancelRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionResponse;
import br.com.systemcommerce.pos.cash.dto.CashSessionSummaryResponse;
import br.com.systemcommerce.pos.cash.dto.PaymentMethodTotal;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cash-sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Cash Sessions", description = "Abertura, conferência e fechamento de caixa (PDV)")
public class CashSessionController {

    private final CashSessionService cashSessionService;

    @PostMapping("/open")
    @PreAuthorize("hasAuthority('POS_OPEN_CASH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre sessão de caixa", description = "Idempotente via header Idempotency-Key")
    public ResponseEntity<ApiResponse<CashSessionResponse>> open(
            @Valid @RequestBody CashSessionOpenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(cashSessionService.open(request, idempotencyKey)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION') or hasAuthority('POS_OPEN_CASH')")
    @Operation(summary = "Consulta sessão atual (OPEN/CLOSING) do terminal")
    public ResponseEntity<ApiResponse<CashSessionResponse>> current(@RequestParam UUID terminalId) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.getCurrent(terminalId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION')")
    @Operation(summary = "Lista sessões de caixa")
    public ResponseEntity<PageResponse<CashSessionResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) CashSession.CashSessionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                cashSessionService.list(storeId, terminalId, operatorId, status, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION')")
    @Operation(summary = "Consulta sessão por ID")
    public ResponseEntity<ApiResponse<CashSessionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.getById(id)));
    }

    @PostMapping("/{id}/start-closing")
    @PreAuthorize("hasAuthority('POS_CLOSE_CASH') or hasAuthority('POS_FORCE_CLOSE_CASH')")
    @Operation(summary = "Inicia fechamento (status CLOSING)")
    public ResponseEntity<ApiResponse<CashSessionResponse>> startClosing(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.startClosing(id)));
    }

    @GetMapping("/{id}/reconciliation")
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION') or hasAuthority('POS_CLOSE_CASH')")
    @Operation(summary = "Simula conferência / valores esperados oficiais")
    public ResponseEntity<ApiResponse<CashReconciliationResponse>> reconciliation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.reconcile(id)));
    }

    @PostMapping("/{id}/conference")
    @PreAuthorize(
            "hasAuthority('POS_VIEW_SESSION') or hasAuthority('POS_CLOSE_CASH') or hasAuthority('POS_FORCE_CLOSE_CASH')")
    @Operation(summary = "Conferência pré-fechamento", description = "Diferenças calculadas na API; não fecha a sessão")
    public ResponseEntity<ApiResponse<CashConferenceResponse>> conference(
            @PathVariable UUID id,
            @Valid @RequestBody CashConferenceRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.conference(id, request)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('POS_CLOSE_CASH') or hasAuthority('POS_FORCE_CLOSE_CASH')")
    @Operation(summary = "Conclui fechamento com valor informado; diferença calculada na API")
    public ResponseEntity<ApiResponse<CashSessionResponse>> close(
            @PathVariable UUID id,
            @Valid @RequestBody CashSessionCloseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.close(id, request, idempotencyKey)));
    }

    @GetMapping("/{id}/closing-receipt")
    @PreAuthorize(
            "hasAuthority('POS_VIEW_SESSION') or hasAuthority('POS_CLOSE_CASH') or hasAuthority('POS_FORCE_CLOSE_CASH')")
    @Operation(summary = "Comprovante oficial de fechamento (após CLOSED)")
    public ResponseEntity<ApiResponse<CashClosingReceiptResponse>> closingReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.closingReceipt(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('POS_OPEN_CASH') or hasAuthority('POS_FORCE_CLOSE_CASH')")
    @Operation(summary = "Cancela abertura quando permitido (sem pagamentos/movimentos extras)")
    public ResponseEntity<ApiResponse<CashSessionResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) CashSessionCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.cancel(
                id, request != null ? request : new CashSessionCancelRequest(null))));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION')")
    @Operation(summary = "Resumo financeiro da sessão")
    public ResponseEntity<ApiResponse<CashSessionSummaryResponse>> summary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.summary(id)));
    }

    @GetMapping("/{id}/expected-by-method")
    @PreAuthorize("hasAuthority('POS_VIEW_SESSION') or hasAuthority('POS_CLOSE_CASH')")
    @Operation(summary = "Valores esperados por forma de pagamento (confirmados)")
    public ResponseEntity<ApiResponse<List<PaymentMethodTotal>>> expectedByMethod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cashSessionService.expectedByPaymentMethod(id)));
    }
}
