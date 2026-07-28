package br.com.systemcommerce.pos.checkout.controller;

import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.SaleChangeResponse;
import br.com.systemcommerce.payment.dto.SalePaymentBalanceResponse;
import br.com.systemcommerce.pos.checkout.dto.PosFinalizeResponse;
import br.com.systemcommerce.pos.checkout.dto.PosFinalizeStatusResponse;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/pos/sales/{saleId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Checkout",
        description =
                """
                Pagamentos e finalização do PDV. Múltiplas formas, troco e totais oficiais na API. \
                Pagamentos confirmados são imutáveis (estorno). Sem PAN/CVV.
                """)
public class PosCheckoutController {

    private final PosCheckoutService posCheckoutService;

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona pagamento à venda PDV")
    public ResponseEntity<ApiResponse<PaymentResponse>> addPayment(
            @PathVariable UUID saleId,
            @Valid @RequestBody PosPaymentAddRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(posCheckoutService.addPayment(saleId, request, idempotencyKey)));
    }

    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Remove pagamento pendente")
    public ResponseEntity<ApiResponse<PaymentResponse>> removePending(
            @PathVariable UUID saleId, @PathVariable UUID paymentId) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.removePending(saleId, paymentId)));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Confirma pagamento (venda já confirmada)")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable UUID saleId, @PathVariable UUID paymentId) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.confirmPayment(saleId, paymentId)));
    }

    @PostMapping("/payments/{paymentId}/refund")
    @PreAuthorize("hasAuthority('POS_PAYMENT_REFUND')")
    @Operation(summary = "Estorna pagamento confirmado")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable UUID saleId,
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.refund(saleId, paymentId, request)));
    }

    @PostMapping("/payments/{paymentId}/refuse")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(
            summary = "Recusa pagamento pendente",
            description = "Ex.: cartão/TEF recusado. Não marca a venda como paga.")
    public ResponseEntity<ApiResponse<PaymentResponse>> refuse(
            @PathVariable UUID saleId,
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.refuse(saleId, paymentId, request)));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Lista pagamentos da venda")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listPayments(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.listPayments(saleId)));
    }

    @GetMapping("/payments/balance")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Consulta saldo restante")
    public ResponseEntity<ApiResponse<SalePaymentBalanceResponse>> balance(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.balance(saleId)));
    }

    @GetMapping("/payments/change")
    @PreAuthorize("hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Calcula troco (somente dinheiro)")
    public ResponseEntity<ApiResponse<SaleChangeResponse>> change(
            @PathVariable UUID saleId, @RequestParam BigDecimal tenderedAmount) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.change(saleId, tenderedAmount)));
    }

    @PostMapping("/finalize")
    @PreAuthorize("hasAuthority('POS_SALE_FINALIZE')")
    @Operation(
            summary = "Finaliza venda PDV",
            description =
                    "Confirma venda (estoque), confirma pagamentos pendentes e exige cobertura total. "
                            + "Transação única; idempotente.")
    public ResponseEntity<ApiResponse<PosFinalizeResponse>> finalizeSale(
            @PathVariable UUID saleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.finalizeSale(saleId, idempotencyKey)));
    }

    @GetMapping("/finalize-status")
    @PreAuthorize("hasAuthority('POS_SALE_FINALIZE') or hasAuthority('POS_PAYMENT_MANAGE')")
    @Operation(summary = "Consulta status da finalização")
    public ResponseEntity<ApiResponse<PosFinalizeStatusResponse>> finalizeStatus(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(posCheckoutService.finalizeStatus(saleId)));
    }
}
