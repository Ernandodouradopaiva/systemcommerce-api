package br.com.systemcommerce.payment.controller;

import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.PaymentStatusHistoryResponse;
import br.com.systemcommerce.payment.dto.SaleChangeResponse;
import br.com.systemcommerce.payment.dto.SaleFinancialSummaryResponse;
import br.com.systemcommerce.payment.dto.SalePaymentBalanceResponse;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "Payments",
        description =
                """
                Pagamentos vinculados a vendas. Status financeiro (PAID / PARTIALLY_PAID), \
                saldo e troco são calculados exclusivamente na API. Pagamentos confirmados \
                não são editados nem excluídos — apenas cancelados com histórico.
                """)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registra pagamento",
            description =
                    "Cria pagamento PENDING (ou confirma na hora com confirmImmediately=true). "
                            + "Valor positivo; totais oficiais na API. Venda cancelada/rascunho rejeitada.")
    public ResponseEntity<ApiResponse<PaymentResponse>> register(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(paymentService.register(request, idempotencyKey)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Consulta pagamento por ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Histórico de status do pagamento")
    public ResponseEntity<ApiResponse<List<PaymentStatusHistoryResponse>>> statusHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.statusHistory(id)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Confirma pagamento", description = "Idempotente. Atualiza status financeiro da venda.")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.confirm(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(
            summary = "Cancela pagamento pendente",
            description = "Idempotente. Confirmados são imutáveis — use estorno (refund).")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancel(
            @PathVariable UUID id, @Valid @RequestBody PaymentCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.cancel(id, request)));
    }

    @PostMapping("/{id}/refuse")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(
            summary = "Recusa pagamento pendente",
            description = "Ex.: cartão recusado. Somente PENDING; não conclui a venda.")
    public ResponseEntity<ApiResponse<PaymentResponse>> refuse(
            @PathVariable UUID id, @Valid @RequestBody PaymentCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.refuse(id, request)));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Estorna pagamento confirmado", description = "Idempotente. Gera CASH_REFUND se dinheiro.")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable UUID id, @Valid @RequestBody PaymentRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.refund(id, request)));
    }

    @GetMapping("/by-sale/{saleId}")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Lista pagamentos da venda")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listBySale(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.listBySale(saleId)));
    }

    @GetMapping("/by-sale/{saleId}/balance")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Consulta saldo a pagar da venda")
    public ResponseEntity<ApiResponse<SalePaymentBalanceResponse>> balance(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.balance(saleId)));
    }

    @GetMapping("/by-sale/{saleId}/change")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(
            summary = "Consulta troco",
            description = "Troco = valor recebido − min(valor recebido, saldo a pagar). Calculado na API.")
    public ResponseEntity<ApiResponse<SaleChangeResponse>> change(
            @PathVariable UUID saleId, @RequestParam BigDecimal tenderedAmount) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.change(saleId, tenderedAmount)));
    }

    @GetMapping("/by-sale/{saleId}/summary")
    @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
    @Operation(summary = "Resumo financeiro da venda")
    public ResponseEntity<ApiResponse<SaleFinancialSummaryResponse>> summary(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.financialSummary(saleId)));
    }
}
