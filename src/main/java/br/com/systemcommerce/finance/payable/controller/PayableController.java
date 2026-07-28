package br.com.systemcommerce.finance.payable.controller;

import br.com.systemcommerce.finance.payable.dto.FinanceGenerationSettingsResponse;
import br.com.systemcommerce.finance.payable.dto.FinanceGenerationSettingsUpdateRequest;
import br.com.systemcommerce.finance.payable.dto.PayableBalanceResponse;
import br.com.systemcommerce.finance.payable.dto.PayableCancelRequest;
import br.com.systemcommerce.finance.payable.dto.PayableCreateRequest;
import br.com.systemcommerce.finance.payable.dto.PayableFromPurchaseRequest;
import br.com.systemcommerce.finance.payable.dto.PayableInstallmentRequest;
import br.com.systemcommerce.finance.payable.dto.PayableInstallmentResponse;
import br.com.systemcommerce.finance.payable.dto.PayableResponse;
import br.com.systemcommerce.finance.payable.dto.PayableSettlementCreateRequest;
import br.com.systemcommerce.finance.payable.dto.PayableSettlementResponse;
import br.com.systemcommerce.finance.payable.entity.Payable;
import br.com.systemcommerce.finance.payable.entity.PayableStatusHistory;
import br.com.systemcommerce.finance.payable.service.FinanceGenerationSettingsService;
import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.finance.payable.service.PayableSettlementService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Payables", description = "Contas a pagar e liquidações (Prompts 96–98)")
public class PayableController {

    private final PayableService payableService;
    private final PayableSettlementService settlementService;
    private final FinanceGenerationSettingsService settingsService;

    @GetMapping("/finance-generation-settings")
    @PreAuthorize("hasAuthority('PAYABLE_READ') or hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<FinanceGenerationSettingsResponse>> getSettings(
            @RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(settingsService.get(organizationId)));
    }

    @PutMapping("/finance-generation-settings")
    @PreAuthorize("hasAuthority('PAYABLE_UPDATE') or hasAuthority('RECEIVABLE_UPDATE')")
    public ResponseEntity<ApiResponse<FinanceGenerationSettingsResponse>> updateSettings(
            @Valid @RequestBody FinanceGenerationSettingsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(settingsService.update(request)));
    }

    @GetMapping("/payables/{id}/cancel-analysis")
    @PreAuthorize("hasAuthority('PAYABLE_CANCEL')")
    public ResponseEntity<ApiResponse<String>> cancelAnalysis(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(payableService.analyzeCancelability(id)));
    }

    @GetMapping("/payables")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<PageResponse<PayableResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) Payable.Status status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(payableService.list(organizationId, supplierId, status, search, pageable)));
    }

    @GetMapping("/payables/agenda")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<List<PayableInstallmentResponse>>> agenda(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.of(payableService.agenda(organizationId, from, to)));
    }

    @GetMapping("/payables/{id}")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<PayableResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(payableService.getById(id)));
    }

    @GetMapping("/payables/{id}/balance")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<PayableBalanceResponse>> balance(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(payableService.balance(id)));
    }

    @GetMapping("/payables/{id}/installments")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<List<PayableInstallmentResponse>>> installments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(payableService.installments(id)));
    }

    @GetMapping("/payables/{id}/history")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<List<PayableStatusHistory>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(payableService.history(id)));
    }

    @PostMapping("/payables")
    @PreAuthorize("hasAuthority('PAYABLE_MANUAL_CREATE')")
    public ResponseEntity<ApiResponse<PayableResponse>> createManual(
            @Valid @RequestBody PayableCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PayableCreateRequest body = idempotencyKey != null && request.idempotencyKey() == null
                ? new PayableCreateRequest(
                        request.organizationId(),
                        request.storeId(),
                        request.supplierId(),
                        request.paymentConditionId(),
                        request.financialCategoryId(),
                        request.costCenterId(),
                        request.documentNumber(),
                        request.issueDate(),
                        request.competenceDate(),
                        request.originalAmount(),
                        request.plannedDiscount(),
                        request.plannedAddition(),
                        request.notes(),
                        idempotencyKey,
                        request.openImmediately(),
                        request.installments())
                : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(payableService.createManual(body)));
    }

    @PostMapping("/payables/from-purchase-receipt")
    @PreAuthorize("hasAuthority('PAYABLE_CREATE')")
    public ResponseEntity<ApiResponse<PayableResponse>> fromPurchase(
            @Valid @RequestBody PayableFromPurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(payableService.generateFromPurchaseReceipt(request)));
    }

    @PutMapping("/payables/{id}")
    @PreAuthorize("hasAuthority('PAYABLE_UPDATE')")
    public ResponseEntity<ApiResponse<PayableResponse>> updateDraft(
            @PathVariable UUID id, @Valid @RequestBody PayableCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(payableService.updateDraft(id, request)));
    }

    @PostMapping("/payables/{id}/cancel")
    @PreAuthorize("hasAuthority('PAYABLE_CANCEL')")
    public ResponseEntity<ApiResponse<PayableResponse>> cancel(
            @PathVariable UUID id, @Valid @RequestBody PayableCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(payableService.cancel(id, request)));
    }

    @PostMapping("/payables/{id}/renegotiate")
    @PreAuthorize("hasAuthority('PAYABLE_RENEGOTIATE')")
    public ResponseEntity<ApiResponse<PayableResponse>> renegotiate(
            @PathVariable UUID id, @Valid @RequestBody List<PayableInstallmentRequest> installments) {
        return ResponseEntity.ok(ApiResponse.of(payableService.renegotiate(id, installments)));
    }

    @PostMapping("/payable-settlements")
    @PreAuthorize("hasAuthority('PAYABLE_SETTLE')")
    public ResponseEntity<ApiResponse<PayableSettlementResponse>> settle(
            @Valid @RequestBody PayableSettlementCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PayableSettlementCreateRequest body = idempotencyKey != null && (request.idempotencyKey() == null || request.idempotencyKey().isBlank())
                ? new PayableSettlementCreateRequest(
                        request.organizationId(),
                        request.storeId(),
                        request.holderId(),
                        request.paymentMethodId(),
                        request.paymentDate(),
                        request.effectiveDate(),
                        request.feeAmount(),
                        request.referenceCode(),
                        request.receiptUrl(),
                        request.notes(),
                        idempotencyKey,
                        request.confirmImmediately(),
                        request.allocations())
                : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(settlementService.settle(body)));
    }

    @PostMapping("/payable-settlements/{id}/confirm")
    @PreAuthorize("hasAuthority('PAYABLE_SETTLE')")
    public ResponseEntity<ApiResponse<PayableSettlementResponse>> confirmSettlement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(settlementService.confirm(id)));
    }

    @GetMapping("/payable-settlements/{id}")
    @PreAuthorize("hasAuthority('PAYABLE_READ')")
    public ResponseEntity<ApiResponse<PayableSettlementResponse>> getSettlement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(settlementService.getById(id)));
    }
}
